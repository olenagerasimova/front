/*
 * The MIT License (MIT) Copyright (c) 2022 artipie.com
 * https://github.com/artipie/front/LICENSE.txt
 */
package com.artipie.front;

import com.artipie.asto.test.TestResource;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test for repository management APIs.
 * @since 0.1
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RepoITCase {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestService service = new TestService()
        .withResource(TestService.CREDS, "RepoITCase/_credentials.yaml")
        .withResource("_api_permissions.yml", "RepoITCase/_api_permissions.yml")
        .withRepoConfigAddingStoragePath("repos/maven-repo.yaml", "RepoITCase/maven-repo.yaml")
        .withRepoConfigAddingStoragePath("repos/pypi-repo.yaml", "RepoITCase/pypi-repo.yaml");

    /**
     * Test http client.
     */
    private TestClient client;

    @BeforeEach
    void init() {
        this.client = new TestClient(this.service.port());
    }

    @Test
    @Timeout(20)
    void canManageRepos() throws InterruptedException {
        final String alice = this.client.token("Alice", "wonderland");
        MatcherAssert.assertThat(
            "Failed to obtain auth token for Alice", alice, new IsNot<>(Matchers.emptyString())
        );
        MatcherAssert.assertThat(
            "Alice failed to get repos info",
            this.client.get("/api/repositories", alice),
            new StringContainsInOrder(Arrays.asList("maven-repo", "pypi-repo"))
        );
        MatcherAssert.assertThat(
            "Alice failed to check maven-repo exists",
            this.client.head("/api/repositories/maven-repo", alice),
            new IsEqual<>(HttpStatus.OK_200)
        );
        MatcherAssert.assertThat(
            "Alice failed to get pypi-repo info",
            this.client.get("/api/repositories/pypi-repo", alice),
            new StringContainsInOrder(Arrays.asList("type", "pypi"))
        );
        final String aladdin = this.client.token("Aladdin", "opensesame");
        MatcherAssert.assertThat(
            "Alice failed to obtain auth token for Aladdin", aladdin,
            new IsNot<>(Matchers.emptyString())
        );
        MatcherAssert.assertThat(
            "Alladin failed to create new repository",
            this.client.put(
                "/api/repositories/rpm-repo", aladdin,
                new String(
                    new TestResource("RepoITCase/request.json").asBytes(),
                    StandardCharsets.UTF_8
                )
            ),
            new IsEqual<>(HttpStatus.CREATED_201)
        );
        MatcherAssert.assertThat(
            "Aladdin failed to check rpm-repo exists",
            this.client.head("/api/repositories/rpm-repo", alice),
            new IsEqual<>(HttpStatus.OK_200)
        );
        MatcherAssert.assertThat(
            "Aladdin failed to delete maven-repo",
            this.client.delete("/api/repositories/maven-repo", aladdin),
            new IsEqual<>(HttpStatus.OK_200)
        );
        this.checkRepoWasRemoved(alice);
        MatcherAssert.assertThat(
            "Alice failed to get repos info",
            this.client.get("/api/repositories", alice),
            new StringContainsInOrder(Arrays.asList("pypi-repo", "rpm-repo"))
        );
    }

    @AfterEach
    void stop() throws IOException {
        this.client.close();
    }

    /**
     * As repository and its data are removed asynchronously, we perform the
     * check several times, waiting a second between the checks. If after 10 checks
     * repo still exists, {@link IllegalStateException} is thrown.
     * @param alice Token for Alice
     * @throws InterruptedException On interrupt
     */
    private void checkRepoWasRemoved(final String alice) throws InterruptedException {
        for (int ind = 0; ind < 10; ind = ind + 1) {
            Logger.info(this, "Checking if maven-repo still exists ...");
            final int status = this.client.head("/api/repositories/maven-repo", alice);
            if (status == 404) {
                Logger.info(this, "Repository fully removed");
                return;
            }
        }
        throw new IllegalStateException("Repository was maven-repo was not removed");
    }

}
