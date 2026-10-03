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
import org.openrewrite.gradle.AddDependency;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.mavenProject;

class AddMicronautValidationDependencyVersionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddMicronautValidationDependencyVersion()).beforeRecipe(withToolingApi());
    }

    @Test
    void unmanagedProcessorUsesExistingValidationVersion() {
        rewriteRun(buildGradle(
          """
            plugins { id 'java' }
            repositories { mavenCentral() }
            dependencies {
                implementation 'io.micronaut.validation:micronaut-validation:4.0.0'
                annotationProcessor 'io.micronaut.validation:micronaut-validation-processor'
            }
            """,
          """
            plugins { id 'java' }
            repositories { mavenCentral() }
            dependencies {
                implementation 'io.micronaut.validation:micronaut-validation:4.0.0'
                annotationProcessor 'io.micronaut.validation:micronaut-validation-processor:4.0.0'
            }
            """));
    }

    @Test
    void unmanagedRuntimeUsesExistingProcessorVersion() {
        rewriteRun(buildGradle(
          """
            plugins { id 'java' }
            repositories { mavenCentral() }
            dependencies {
                testImplementation 'io.micronaut.validation:micronaut-validation'
                testAnnotationProcessor 'io.micronaut.validation:micronaut-validation-processor:4.0.0'
            }
            """,
          """
            plugins { id 'java' }
            repositories { mavenCentral() }
            dependencies {
                testImplementation 'io.micronaut.validation:micronaut-validation:4.0.0'
                testAnnotationProcessor 'io.micronaut.validation:micronaut-validation-processor:4.0.0'
            }
            """));
    }

    @Test
    void runtimeOnlyManagementDoesNotCoverCompileClasspath() {
        rewriteRun(buildGradle(
          """
            plugins { id 'java' }
            repositories { mavenCentral() }
            dependencies {
                runtimeOnly platform('io.micronaut.validation:micronaut-validation-bom:4.0.0')
                implementation 'io.micronaut.validation:micronaut-validation'
                annotationProcessor 'io.micronaut.validation:micronaut-validation-processor:4.0.0'
            }
            """,
          """
            plugins { id 'java' }
            repositories { mavenCentral() }
            dependencies {
                runtimeOnly platform('io.micronaut.validation:micronaut-validation-bom:4.0.0')
                implementation 'io.micronaut.validation:micronaut-validation:4.0.0'
                annotationProcessor 'io.micronaut.validation:micronaut-validation-processor:4.0.0'
            }
            """));
    }

    @Test
    void preserveResolutionStrategyVersion() {
        rewriteRun(buildGradle(
          """
            plugins { id 'java' }
            repositories { mavenCentral() }
            configurations.all {
                resolutionStrategy.eachDependency { details ->
                    if (details.requested.group == 'io.micronaut.validation') {
                        details.useVersion '4.0.0'
                    }
                }
            }
            dependencies {
                implementation 'io.micronaut.validation:micronaut-validation'
            }
            """));
    }

    @Test
    void preservePlatformManagedVersion() {
        rewriteRun(buildGradle(
          """
            plugins { id 'java' }
            repositories { mavenCentral() }
            dependencies {
                implementation platform('io.micronaut.validation:micronaut-validation-bom:4.0.0')
                implementation 'io.micronaut.validation:micronaut-validation'
                annotationProcessor platform('io.micronaut.validation:micronaut-validation-bom:4.0.0')
                annotationProcessor 'io.micronaut.validation:micronaut-validation-processor'
            }
            """));
    }

    @Test
    void preserveConstraintManagedVersion() {
        rewriteRun(buildGradle(
          """
            plugins { id 'java' }
            repositories { mavenCentral() }
            dependencies {
                constraints {
                    implementation 'io.micronaut.validation:micronaut-validation:4.0.0'
                }
                implementation 'io.micronaut.validation:micronaut-validation'
            }
            """));
    }

    @Test
    void newlyAddedProcessorKeepsPlatformManagement() {
        rewriteRun(spec -> spec.recipes(
            new AddDependency("io.micronaut.validation", "micronaut-validation-processor", null, null,
              "annotationProcessor", null, null, null, null, null),
            new AddMicronautValidationDependencyVersion()),
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              dependencies {
                  annotationProcessor platform('io.micronaut.validation:micronaut-validation-bom:4.0.0')
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              dependencies {
                  annotationProcessor platform('io.micronaut.validation:micronaut-validation-bom:4.0.0')
                  annotationProcessor "io.micronaut.validation:micronaut-validation-processor"
              }
              """));
    }

    @Test
    void reuseValidationVersionAcrossModules() {
        rewriteRun(
          mavenProject("library", buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              dependencies {
                  implementation 'io.micronaut.validation:micronaut-validation:4.0.0'
              }
              """)),
          mavenProject("app", buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              dependencies {
                  annotationProcessor 'io.micronaut.validation:micronaut-validation-processor'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              dependencies {
                  annotationProcessor 'io.micronaut.validation:micronaut-validation-processor:4.0.0'
              }
              """)));
    }

}
