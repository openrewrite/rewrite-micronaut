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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

public class RemoveUnnecessaryDependenciesTest implements RewriteTest {

    private static String latestPluginVersion;

    private static String latestMicronautVersion;

    private static SourceSpecs gradleProperties;

    @BeforeAll
    static void init() throws MavenDownloadingException {
        ExecutionContext ctx = new InMemoryExecutionContext();
        latestPluginVersion = MicronautVersionHelper.getLatestMN4PluginVersion("io.micronaut.application");
        latestMicronautVersion = MicronautVersionHelper.getLatestMN4Version();
        gradleProperties = properties("micronautVersion=" + latestMicronautVersion, s -> s.path("gradle.properties"));
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.RemoveUnnecessaryDependencies");
    }

    @Test
    void gradleDependenciesRemoved() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()), mavenProject("project",
          gradleProperties,
          //language=groovy
          buildGradle(String.format("""
            plugins {
                id("io.micronaut.application") version "%s"
            }
                        
            repositories {
                mavenCentral()
            }
                        
            dependencies {
                implementation "io.micronaut:micronaut-runtime"
            }
            """, latestPluginVersion), String.format("""
            plugins {
                id("io.micronaut.application") version "%s"
            }
                        
            repositories {
                mavenCentral()
            }
                        
            dependencies {
            }
            """, latestPluginVersion))));
    }

    @Test
    void mavenDependenciesRemoved() {
        rewriteRun(mavenProject("project",
          //language=xml
          pomXml(String.format("""
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
                        <artifactId>micronaut-runtime</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """, latestMicronautVersion), String.format("""
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
            """, latestMicronautVersion))));
    }
}
