/*
 * The MIT License (MIT) Copyright (c) 2022 artipie.com
 * https://github.com/artipie/front/LICENSE.txt
 */
package com.artipie.front.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.front.api.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.Json;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link YamlStorages}.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class YamlStoragesTest {

    /**
     * Test storage.
     */
    private BlockingStorage blsto;

    @BeforeEach
    void init() {
        this.blsto = new BlockingStorage(new InMemoryStorage());
    }

    @ParameterizedTest
    @CsvSource({
        "_storages.yaml,my-repo",
        "_storages.yml,my-repo",
        "_storages.yaml,",
        "_storages.yml,"
    })
    void readsStorages(final String stn, final String repo) {
        final String[] aliases = new String[]{"default", "hdd", "extra"};
        final Optional<Key> key = Optional.ofNullable(repo).map(Key.From::new);
        this.createSettings(
            key.map(val -> new Key.From(val, stn)).orElse(new Key.From(stn)), aliases
        );
        MatcherAssert.assertThat(
            new YamlStorages(key, this.blsto).list().stream().map(Storages.Storage::alias)
                .collect(Collectors.toList()),
            Matchers.containsInAnyOrder(aliases)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "_storages.yaml,my-repo",
        "_storages.yml,my-repo",
        "_storages.yaml,",
        "_storages.yml,"
    })
    void removesStorage(final String stn, final String repo) {
        final String[] aliases = new String[]{"one", "two", "three"};
        final Optional<Key> key = Optional.ofNullable(repo).map(Key.From::new);
        this.createSettings(
            key.map(val -> new Key.From(val, stn)).orElse(new Key.From(stn)), aliases
        );
        final YamlStorages storages = new YamlStorages(key, this.blsto);
        storages.remove("two");
        MatcherAssert.assertThat(
            storages.list().stream().map(Storages.Storage::alias).collect(Collectors.toList()),
            Matchers.containsInAnyOrder("one", "three")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "_storages.yaml,my-repo",
        "_storages.yml,my-repo",
        "_storages.yaml,",
        "_storages.yml,"
    })
    void addsStorage(final String stn, final String repo) {
        final Optional<Key> key = Optional.ofNullable(repo).map(Key.From::new);
        final String def = "default";
        this.createSettings(key.map(val -> new Key.From(val, stn)).orElse(new Key.From(stn)), def);
        final YamlStorages storages = new YamlStorages(key, this.blsto);
        final String another = "newOne";
        storages.add(another, Json.createObjectBuilder().add("type", "s3").build());
        MatcherAssert.assertThat(
            storages.list().stream().map(Storages.Storage::alias).collect(Collectors.toList()),
            Matchers.containsInAnyOrder(def, another)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void addsWhenDoesNotExists(final boolean repo) {
        Optional<Key> key = Optional.empty();
        if (repo) {
            key = Optional.of(new Key.From("some-repo"));
        }
        final YamlStorages storages = new YamlStorages(key, this.blsto);
        final String another = "newOne";
        storages.add(another, Json.createObjectBuilder().add("type", "file").build());
        MatcherAssert.assertThat(
            storages.list().stream().map(Storages.Storage::alias).collect(Collectors.toList()),
            Matchers.containsInAnyOrder(another)
        );
    }

    @Test
    void throwsExceptionIfRepoDoesNotExist() {
        Assertions.assertThrows(
            NotFoundException.class,
            () -> new YamlStorages(this.blsto).remove("any")
        );
    }

    void createSettings(final Key key, final String... aliases) {
        YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
        for (final String alias : aliases) {
            builder = builder.add(
                alias,
                Yaml.createYamlMappingBuilder().add("type", "file")
                    .add("path", String.format("/data/%s", alias)).build()
            );
        }
        this.blsto.save(
            key,
            Yaml.createYamlMappingBuilder().add("storages", builder.build())
            .build().toString().getBytes(StandardCharsets.UTF_8)
        );
    }

}
