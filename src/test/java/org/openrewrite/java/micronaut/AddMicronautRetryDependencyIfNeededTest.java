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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

public class AddMicronautRetryDependencyIfNeededTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),"jakarta.inject-api-2.*", "micronaut-retry-4.*"));
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

    private final SourceSpecs gradleProperties = properties("micronautVersion=4.0.0-M4", s -> s.path("gradle.properties"));

    @Language("groovy")
    private final String buildGradleInitial = """
            plugins {
                id("io.micronaut.application") version "4.0.0-M8"
            }
            
            repositories {
                mavenCentral()
            }
        """;

    @Language("groovy")
    private final String buildGradleExpected = """
            plugins {
                id("io.micronaut.application") version "4.0.0-M8"
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation "io.micronaut:micronaut-retry"
            }
        """;

    @Language("xml")
    private final String pomInitial = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>io.micronaut.platform</groupId>
                    <artifactId>micronaut-parent</artifactId>
                    <version>4.0.0-M4</version>
                </parent>
            </project>
        """;

    @Language("xml")
    private final String pomExpected = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>io.micronaut.platform</groupId>
                    <artifactId>micronaut-parent</artifactId>
                    <version>4.0.0-M4</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>io.micronaut</groupId>
                        <artifactId>micronaut-retry</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """;

    @Test
    void updateGradleDependencies() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()), mavenProject("project", srcMainJava(java(retryableService)), gradleProperties, buildGradle(buildGradleInitial, buildGradleExpected)));
    }

    @Test
    void updateMavenDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(retryableService)), pomXml(pomInitial, pomExpected)));
    }
}
