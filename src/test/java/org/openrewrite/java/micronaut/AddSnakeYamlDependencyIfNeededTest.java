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

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

public class AddSnakeYamlDependencyIfNeededTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "micronaut-context-4.*"));
    }

    @Language("java")
    private final String micronautApplication = """
            import io.micronaut.runtime.Micronaut;
            
            public class Application {
            
                public static void main(String[] args) {
                    Micronaut.run(Application.class, args);
                }
            }
      """;

    @Language("yml")
    private final String micronautConfig = """
        micronaut:
            application:
                name: testApp
        """;

    @Language("properties")
    private final String micronautPropertiesConfig = """
            micronaut.application.name=testApp
        """;

    @Language("groovy")
    private final String buildGradleNoDependency = """
            plugins {
                id("io.micronaut.application") version "4.0.0-M2"
            }
            
            micronaut { version '4.0.0-M2'}
            
            repositories {
                mavenCentral()
            }
        """;

    @Language("groovy")
    private final String buildGradleWithDependency = """
            plugins {
                id("io.micronaut.application") version "4.0.0-M2"
            }
            
            micronaut { version '4.0.0-M2'}
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                runtimeOnly "org.yaml:snakeyaml"
            }
        """;

    @Language("xml")
    private final String initialPom = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>io.micronaut.platform</groupId>
                    <artifactId>micronaut-parent</artifactId>
                    <version>4.0.0-M2</version>
                </parent>
            </project>
        """;

    @Language("xml")
    private final String pomWithDependency = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>io.micronaut.platform</groupId>
                    <artifactId>micronaut-parent</artifactId>
                    <version>4.0.0-M2</version>
                </parent>
                <dependencies>
                    <dependency>
                        <groupId>org.yaml</groupId>
                        <artifactId>snakeyaml</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
            </project>
        """;

    @Test
    void testAddGradleDependencyForApplicationYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                        srcMainJava(java(micronautApplication)),
                        srcMainResources(yaml(micronautConfig, s -> s.path("application.yml"))),
                buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void testAddMavenDependencyForApplicationYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("application.yml"))),
                pomXml(initialPom, pomWithDependency)));
    }

    @Test
    void testAddGradleDependencyForApplicationYaml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("application.yaml"))),
                buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void testAddMavenDependencyForApplicationYaml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("application.yaml"))),
                pomXml(initialPom, pomWithDependency)));
    }


    @Test
    void testNoGradleDependencyForMissingApplicationYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("foo.yml"))),
                buildGradle(buildGradleNoDependency)));
    }

    @Test
    void testNoMavenDependencyForMissingApplicationYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("foo.yml"))),
                pomXml(initialPom)));
    }

    @Test
    void testNoGradleDependencyForApplicationProperties() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(properties(micronautPropertiesConfig, s -> s.path("application.properties"))),
                buildGradle(buildGradleNoDependency)));
    }

    @Test
    void testNoMavenDependencyForApplicationProperties() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(properties(micronautPropertiesConfig, s -> s.path("application.properties"))),
                pomXml(initialPom)));
    }

    @Test
    void testExistingGradleDependencyUnchanged() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("application.yml"))),
                buildGradle(buildGradleWithDependency)));
    }

    @Test
    void testExistingMavenDependencyUnchanged() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("application.yml"))),
                pomXml(pomWithDependency)));
    }

    @Test
    void testAddGradleDependencyForEnvironmentYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("application-foo.yml"))),
                buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void testAddMavenDependencyForEnvironmentYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("application-foo.yml"))),
                pomXml(initialPom, pomWithDependency)));
    }

    @Test
    void testAddGradleDependencyForTestYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcTestResources(yaml(micronautConfig, s -> s.path("application-test.yml"))),
                buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void testAddMavenDependencyForTestYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcTestResources(yaml(micronautConfig, s -> s.path("application-test.yml"))),
                pomXml(initialPom, pomWithDependency)));
    }

    @Test
    void testAddGradleDependencyForBootstrapYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("bootstrap.yml"))),
                buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void testAddMavenDependencyForBootstrapYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("bootstrap.yml"))),
                pomXml(initialPom, pomWithDependency)));
    }

    @Test
    void testAddGradleDependencyForBootstrapEnvironmentYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("bootstrap-foo.yml"))),
                buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void testAddMavenDependencyForBootstrapEnvironmentYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
                srcMainJava(java(micronautApplication)),
                srcMainResources(yaml(micronautConfig, s -> s.path("bootstrap-foo.yml"))),
                pomXml(initialPom, pomWithDependency)));
    }
}
