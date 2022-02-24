/*
 * The MIT License (MIT) Copyright (c) 2022 artipie.com
 * https://github.com/artipie/front/LICENSE.txt
 */
package com.artipie.front.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.front.auth.Credentials;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Artipie yaml settings.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class ArtipieYaml {

    /**
     * YAML file content.
     */
    private final YamlMapping content;

    /**
     * Ctor.
     * @param content YAML file content
     */
    public ArtipieYaml(final YamlMapping content) {
        this.content = content;
    }

    /**
     * Artipie storage.
     * @return Storage
     */
    public BlockingStorage storage() {
        return new BlockingStorage(this.asto());
    }

    /**
     * Artipie layout: flat or org.
     * @return Layout value
     */
    public String layout() {
        return Optional.ofNullable(this.meta().string("layout")).orElse("flat");
    }

    /**
     * Yaml settings meta section.
     * @return Meta yaml section
     */
    public YamlMapping meta() {
        return this.content.yamlMapping("meta");
    }

    /**
     * Repository configurations storage.
     * @return Storage, where repo configs are located
     */
    public BlockingStorage repoConfigsStorage() {
        return new BlockingStorage(
            Optional.ofNullable(this.meta().string("repo_configs"))
            .<Storage>map(str -> new SubStorage(new Key.From(str), this.asto()))
            .orElse(this.asto())
        );
    }

    /**
     * Credentials from config.
     * @return Credentials
     */
    public Credentials credentials() {
        final var cred = this.content.yamlMapping("credentials");
        if (!cred.string("type").equals("file")) {
            throw new NotImplementedException("not implemented yet");
        }
        try {
            return new YamlCredentials(
                Yaml.createYamlInput(
                    new String(
                        this.storage().value(new Key.From(cred.string("path"))),
                        StandardCharsets.UTF_8
                    )
                ).readYamlMapping()
            );
        } catch (final IOException iex) {
            throw new UncheckedIOException(iex);
        }
    }

    @Override
    public String toString() {
        return String.format("YamlSettings{\n%s\n}", this.content.toString());
    }

    /**
     * Abstract storage.
     * @return Asto
     */
    private Storage asto() {
        return new YamlStorage(
            Optional.ofNullable(
                this.meta().yamlMapping("storage")
            ).orElseThrow(
                () -> new ArtipieException(
                    String.format(
                        "Failed to find storage configuration in \n%s", this.content.toString()
                    )
                )
            )
        ).storage();
    }
}
