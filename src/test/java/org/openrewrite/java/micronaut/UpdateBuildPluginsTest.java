/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.micronaut;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

public class UpdateBuildPluginsTest extends Micronaut4RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.micronaut")
          .build()
          .activateRecipes( "org.openrewrite.java.micronaut.UpdateMicronautPlatformBom", "org.openrewrite.java.micronaut.UpdateBuildPlugins"));
    }

    @DocumentExample
    @Test
    void updateGradleBuildPlugins() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()), properties("micronautVersion=3.9.1", s -> s.path("gradle.properties")),
          //language=groovy
          buildGradle("""
                plugins {
                    id("com.github.johnrengelman.shadow") version "7.1.2"
                    id("io.micronaut.application") version "3.7.9"
                    id("io.micronaut.minimal.application") version "3.7.9"
                    id("io.micronaut.aot") version "3.7.9"
                    id("io.micronaut.component") version "3.7.9"
                    id("io.micronaut.crac") version "3.7.9"
                    id("io.micronaut.docker") version "3.7.9"
                    id("io.micronaut.graalvm") version "3.7.9"
                    id("io.micronaut.library") version "3.7.9"
                    id("io.micronaut.minimal.library") version "3.7.9"
                    id("io.micronaut.test-resources") version "3.5.1"
                }
                
                repositories {
                    mavenCentral()
                }
            """, """
                plugins {
                    id("com.github.johnrengelman.shadow") version "8.1.1"
                    id("io.micronaut.application") version "%s"
                    id("io.micronaut.minimal.application") version "%s"
                    id("io.micronaut.aot") version "%s"
                    id("io.micronaut.component") version "%s"
                    id("io.micronaut.crac") version "%s"
                    id("io.micronaut.docker") version "%s"
                    id("io.micronaut.graalvm") version "%s"
                    id("io.micronaut.library") version "%s"
                    id("io.micronaut.minimal.library") version "%s"
                    id("io.micronaut.test-resources") version "%s"
                }
                
                repositories {
                    mavenCentral()
                }
            """.formatted(latestApplicationPluginVersion, MicronautRewriteTestVersions.getLatestMN4PluginVersion("io.micronaut.minimal.application"), MicronautRewriteTestVersions.getLatestMN4PluginVersion("io.micronaut.aot"), MicronautRewriteTestVersions.getLatestMN4PluginVersion("io.micronaut.component"), MicronautRewriteTestVersions.getLatestMN4PluginVersion("io.micronaut.crac"), MicronautRewriteTestVersions.getLatestMN4PluginVersion("io.micronaut.docker"), MicronautRewriteTestVersions.getLatestMN4PluginVersion("io.micronaut.graalvm"), MicronautRewriteTestVersions.getLatestMN4PluginVersion("io.micronaut.library"), MicronautRewriteTestVersions.getLatestMN4PluginVersion("io.micronaut.minimal.library"), MicronautRewriteTestVersions.getLatestMN4PluginVersion("io.micronaut.test-resources"))));
    }

    @Test
    void updateMavenBuildPlugin() {
        rewriteRun(mavenProject("project",
          //language=xml
          pomXml("""
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <parent>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-parent</artifactId>
                        <version>3.9.1</version>
                    </parent>
                    <build>
                      <plugins>
                          <plugin>
                              <groupId>io.micronaut.build</groupId>
                              <artifactId>micronaut-maven-plugin</artifactId>
                          </plugin>
                      </plugins>
                    </build>
                </project>    
            """, """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <parent>
                        <groupId>io.micronaut.platform</groupId>
                        <artifactId>micronaut-parent</artifactId>
                        <version>%s</version>
                    </parent>
                    <build>
                      <plugins>
                          <plugin>
                              <groupId>io.micronaut.maven</groupId>
                              <artifactId>micronaut-maven-plugin</artifactId>
                          </plugin>
                      </plugins>
                    </build>
                </project>
            """.formatted(latestMicronautVersion))));
    }
}
