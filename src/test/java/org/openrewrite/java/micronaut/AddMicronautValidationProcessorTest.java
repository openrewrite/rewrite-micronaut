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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.groovy.GroovyParser;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Collections;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.kotlin.Assertions.kotlin;

class AddMicronautValidationProcessorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddMicronautValidationProcessor())
          .parser(GroovyParser.builder().classpathFromResource(new InMemoryExecutionContext(), "jakarta.validation-api-3.*"))
          .parser(KotlinParser.builder().classpathFromResources(new InMemoryExecutionContext(), "jakarta.validation-api-3.*"));
    }

    @ParameterizedTest
    @CsvSource({"main,compileOnly", "test,testCompileOnly"})
    void groovyProcessorConfiguration(String sourceSet, String configuration) {
        String build = """
          plugins {
              id 'groovy'
          }
          repositories {
              mavenCentral()
          }
          """;
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()),
          mavenProject("project",
            groovy("""
              import jakarta.validation.constraints.NotBlank
              class Person {
                  @NotBlank String name
              }
              """, s -> s.path("src/" + sourceSet + "/groovy/Person.groovy")
              .markers(new JavaSourceSet(randomId(), sourceSet, Collections.emptyList(), Collections.emptyMap()))),
            buildGradle(build, build + """

              dependencies {
                  %s "io.micronaut.validation:micronaut-validation-processor"
              }
              """.formatted(configuration))));
    }

    @ParameterizedTest
    @CsvSource({"main,kapt", "test,kaptTest"})
    void kotlinProcessorConfiguration(String sourceSet, String configuration) {
        String build = """
          plugins {
              id 'java'
          }
          repositories {
              mavenCentral()
          }
          configurations {
              kapt
              kaptTest
          }
          """;
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()),
          mavenProject("project",
            kotlin("""
              import jakarta.validation.constraints.NotBlank
              class Person(@field:NotBlank val name: String)
              """, s -> s.path("src/" + sourceSet + "/kotlin/Person.kt")
              .markers(new JavaSourceSet(randomId(), sourceSet, Collections.emptyList(), Collections.emptyMap()))),
            buildGradle(build, build + """

              dependencies {
                  %s "io.micronaut.validation:micronaut-validation-processor"
              }
              """.formatted(configuration))));
    }
}
