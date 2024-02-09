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
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

class AddSnakeYamlDependencyIfNeededTest extends Micronaut4RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "micronaut-context-4.*", "micronaut-inject-4.*"));
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

    private final String buildGradleNoDependency =
            //language=groovy
            """
            plugins {
                id("io.micronaut.application") version "%s"
            }
            
            repositories {
                mavenCentral()
            }
        """.formatted(latestApplicationPluginVersion);

    private final String buildGradleWithDependency =
            //language=groovy
            """
            plugins {
                id("io.micronaut.application") version "%s"
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                runtimeOnly "org.yaml:snakeyaml"
            }
        """.formatted(latestApplicationPluginVersion);

    private final String initialPom =
            //language=xml
            """
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
        """.formatted(latestMicronautVersion);

    private final String pomWithDependency =
            //language=xml
            """
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
                        <groupId>org.yaml</groupId>
                        <artifactId>snakeyaml</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
            </project>
        """.formatted(latestMicronautVersion);

    @Test
    void addGradleDependencyForApplicationYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("application.yml"))),
          getGradleProperties(),
          buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void addMavenDependencyForApplicationYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("application.yml"))),
          pomXml(initialPom, pomWithDependency)));
    }

    @Test
    void addGradleDependencyForApplicationYaml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("application.yaml"))),
          getGradleProperties(),
          buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void addMavenDependencyForApplicationYaml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("application.yaml"))),
          pomXml(initialPom, pomWithDependency)));
    }


    @Test
    void noGradleDependencyForMissingApplicationYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("foo.yml"))),
          getGradleProperties(),
          buildGradle(buildGradleNoDependency)));
    }

    @Test
    void noMavenDependencyForMissingApplicationYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("foo.yml"))),
          pomXml(initialPom)));
    }

    @Test
    void noGradleDependencyForApplicationProperties() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(properties(micronautPropertiesConfig, s -> s.path("application.properties"))),
          getGradleProperties(),
          buildGradle(buildGradleNoDependency)));
    }

    @Test
    void noMavenDependencyForApplicationProperties() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(properties(micronautPropertiesConfig, s -> s.path("application.properties"))),
          pomXml(initialPom)));
    }

    @Test
    void existingGradleDependencyUnchanged() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("application.yml"))),
          getGradleProperties(),
          buildGradle(buildGradleWithDependency)));
    }

    @Test
    void existingMavenDependencyUnchanged() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("application.yml"))),
          pomXml(pomWithDependency)));
    }

    @Test
    void addGradleDependencyForEnvironmentYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("application-foo.yml"))),
          getGradleProperties(),
          buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void addMavenDependencyForEnvironmentYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("application-foo.yml"))),
          pomXml(initialPom, pomWithDependency)));
    }

    @Test
    void addGradleDependencyForTestYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcTestResources(yaml(micronautConfig, s -> s.path("application-test.yml"))),
          getGradleProperties(),
          buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void addMavenDependencyForTestYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcTestResources(yaml(micronautConfig, s -> s.path("application-test.yml"))),
          pomXml(initialPom, pomWithDependency)));
    }

    @Test
    void addGradleDependencyForBootstrapYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("bootstrap.yml"))),
          getGradleProperties(),
          buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void addMavenDependencyForBootstrapYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("bootstrap.yml"))),
          pomXml(initialPom, pomWithDependency)));
    }

    @Test
    void addGradleDependencyForBootstrapEnvironmentYml() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()).recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("bootstrap-foo.yml"))),
          getGradleProperties(),
          buildGradle(buildGradleNoDependency, buildGradleWithDependency)));
    }

    @Test
    void addMavenDependencyForBootstrapEnvironmentYml() {
        rewriteRun(spec -> spec.recipe(new AddSnakeYamlDependencyIfNeeded()), mavenProject("project",
          srcMainJava(java(micronautApplication)),
          srcMainResources(yaml(micronautConfig, s -> s.path("bootstrap-foo.yml"))),
          pomXml(initialPom, pomWithDependency)));
    }
}
