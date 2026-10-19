/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class Micronaut4to5MigrationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "micronaut-core-2.*"));
        spec.recipeFromResources("org.openrewrite.java.micronaut.Micronaut4to5Migration");
    }

    @DocumentExample
    @Test
    void migratesMicronautNullableAnnotationToJSpecify() {
        rewriteRun(
          //language=java
          java(
            """
              import io.micronaut.core.annotation.Nullable;

              class Service {
                  @Nullable
                  String name;
              }
              """,
            """
              import org.jspecify.annotations.Nullable;

              class Service {
                  @Nullable
                  String name;
              }
              """
          )
        );
    }

    @Test
    void securityAnnotationsWithExplicitVersionMoveToProcessor() {
        rewriteRun(
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.micronaut.security</groupId>
                            <artifactId>micronaut-security-annotations</artifactId>
                            <version>4.18.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(actual -> assertThat(actual)
                .containsPattern("<groupId>io.micronaut.security</groupId>\\s*<artifactId>micronaut-security-processor</artifactId>\\s*<version>5\\.\\d+\\.\\d+</version>")
                .actual())
            )
          )
        );
    }

    @Test
    void eclipsestoreAnnotationsWithExplicitVersionMoveToProcessor() {
        rewriteRun(
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.micronaut.eclipsestore</groupId>
                            <artifactId>micronaut-eclipsestore-annotations</artifactId>
                            <version>1.10.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(actual -> assertThat(actual)
                .containsPattern("<groupId>io.micronaut.eclipsestore</groupId>\\s*<artifactId>micronaut-eclipsestore-processor</artifactId>\\s*<version>2\\.\\d+\\.\\d+</version>")
                .actual())
            )
          )
        );
    }

    @Test
    void testcontainersJunitJupiterWithExplicitVersionMovesToRenamedModule() {
        rewriteRun(
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.testcontainers</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <version>1.21.4</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(actual -> assertThat(actual)
                .containsPattern("<groupId>org.testcontainers</groupId>\\s*<artifactId>testcontainers-junit-jupiter</artifactId>\\s*<version>2\\.\\d+\\.\\d+</version>")
                .actual())
            )
          )
        );
    }
}
