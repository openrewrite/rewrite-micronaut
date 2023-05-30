package org.openrewrite.java.micronaut;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

public class UpdateBuildPluginsTest implements RewriteTest {

    @Language("groovy")
    private final String initialBuild = """
            plugins {
                id("com.github.johnrengelman.shadow") version "7.1.2"
                id("io.micronaut.application") version "3.7.9"
                id("io.micronaut.minimal.application") version "3.7.9"
                id("io.micronaut.aot") version "3.7.9"
                id("io.micronaut.crac") version "3.7.9"
                id("io.micronaut.docker") version "3.7.9"
                id("io.micronaut.graalvm") version "3.7.9"
                id("io.micronaut.library") version "3.7.9"
                id("io.micronaut.minimal.library") version "3.7.9"
                id("io.micronaut.test-resources") version "3.5.1"
                id("io.micronaut.test-resources-consumer") version "3.5.1"
            }
        """;

    @Language("groovy")
    private final String updatedPlugins = """
            plugins {
                id("com.github.johnrengelman.shadow") version "8.1.1"
                id("io.micronaut.application") version "4.0.0-M2"
                id("io.micronaut.minimal.application") version "4.0.0-M2"
                id("io.micronaut.aot") version "4.0.0-M2"
                id("io.micronaut.crac") version "4.0.0-M2"
                id("io.micronaut.docker") version "4.0.0-M2"
                id("io.micronaut.graalvm") version "4.0.0-M2"
                id("io.micronaut.library") version "4.0.0-M2"
                id("io.micronaut.minimal.library") version "4.0.0-M2"
                id("io.micronaut.test-resources") version "4.0.0-M2"
                id("io.micronaut.test-resources-consumer") version "4.0.0-M2"
            }
        """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.UpdateBuildPlugins");
    }

    @Test
    void updateBuildPlugins() {
        rewriteRun(buildGradle(initialBuild, updatedPlugins));
    }
}
