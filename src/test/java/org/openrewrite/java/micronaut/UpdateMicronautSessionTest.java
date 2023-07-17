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

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

public class UpdateMicronautSessionTest extends Micronaut4RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.UpdateMicronautPlatformBom", "org.openrewrite.java.micronaut.UpdateMicronautSession");
    }

    @Test
    void updateGradleDependencies() {
        rewriteRun(mavenProject("project",
          //language=groovy
          buildGradle("""
                dependencies {
                    implementation("io.micronaut:micronaut-session")
                }
            """, """
                dependencies {
                    implementation("io.micronaut.session:micronaut-session")
                }
            """)));
    }

    @Test
    void updateMavenDependencies() {
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
                        <version>%s</version>
                    </parent>
                    <dependencies>
                        <dependency>
                            <groupId>io.micronaut</groupId>
                            <artifactId>micronaut-session</artifactId>
                        </dependency>
                    </dependencies>
                </project>
            """.formatted(MicronautRewriteTestVersions.getLatestMN3Version()), """
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
                            <groupId>io.micronaut.session</groupId>
                            <artifactId>micronaut-session</artifactId>
                        </dependency>
                    </dependencies>
                </project>
            """.formatted(latestMicronautVersion))));
    }
}
