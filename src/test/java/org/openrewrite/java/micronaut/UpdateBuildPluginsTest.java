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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

public class UpdateBuildPluginsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.UpdateMicronautPlatformBom",
          "org.openrewrite.java.micronaut.UpdateBuildPlugins");
    }

    @Test
    void updateGradleBuildPlugins() {
        rewriteRun(
          //language=groovy
          buildGradle("""
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
            """, """
                plugins {
                    id("com.github.johnrengelman.shadow") version "8.1.1"
                    id("io.micronaut.application") version "4.0.0-M4"
                    id("io.micronaut.minimal.application") version "4.0.0-M4"
                    id("io.micronaut.aot") version "4.0.0-M4"
                    id("io.micronaut.crac") version "4.0.0-M4"
                    id("io.micronaut.docker") version "4.0.0-M4"
                    id("io.micronaut.graalvm") version "4.0.0-M4"
                    id("io.micronaut.library") version "4.0.0-M4"
                    id("io.micronaut.minimal.library") version "4.0.0-M4"
                    id("io.micronaut.test-resources") version "4.0.0-M4"
                    id("io.micronaut.test-resources-consumer") version "4.0.0-M4"
                } 
            """));
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
                            <version>4.0.0-M3</version>
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
                """)));
    }
}
