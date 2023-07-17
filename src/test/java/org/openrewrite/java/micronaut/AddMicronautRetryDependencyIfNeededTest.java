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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

public class AddMicronautRetryDependencyIfNeededTest extends Micronaut4RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jakarta.inject-api-2.*", "micronaut-retry-4.*"));
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.AddMicronautRetryDependencyIfNeeded");
    }

    @Language("java")
    private final String retryableService = """
          import jakarta.inject.Singleton;
          import io.micronaut.retry.annotation.Retryable;
          
          @Singleton
          public class PersonService {
          
              @Retryable
              public void callFlakyRemote() {
                  
              }
          }
      """;

    @Test
    void updateGradleDependencies() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()), mavenProject("project", srcMainJava(java(retryableService)), getGradleProperties(),
          //language=groovy
          buildGradle("""
                plugins {
                    id("io.micronaut.application") version "%s"
                }
                
                repositories {
                    mavenCentral()
                }
            """.formatted(latestApplicationPluginVersion), """
                plugins {
                    id("io.micronaut.application") version "%s"
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "io.micronaut:micronaut-retry"
                }
            """.formatted(latestApplicationPluginVersion))));
    }

    @Test
    void updateMavenDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(retryableService)),
          //language=xml
          pomXml("""
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <parent>
                        <groupId>io.micronaut.platform</groupId>
                        <artifactId>micronaut-parent</artifactId>
                        <version>%s</version>
                    </parent>
                </project>
            """.formatted(latestMicronautVersion), """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <parent>
                        <groupId>io.micronaut.platform</groupId>
                        <artifactId>micronaut-parent</artifactId>
                        <version>%s</version>
                    </parent>
                    <dependencies>
                        <dependency>
                            <groupId>io.micronaut</groupId>
                            <artifactId>micronaut-retry</artifactId>
                        </dependency>
                    </dependencies>
                </project>
            """.formatted(latestMicronautVersion))));
    }
}
