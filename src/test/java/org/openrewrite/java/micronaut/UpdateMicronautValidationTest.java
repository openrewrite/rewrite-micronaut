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
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class UpdateMicronautValidationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "validation-api-2.*", "jakarta.validation-api-3.*", "jakarta.inject-api-2.*"))
          .recipes(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.micronaut")
            .build()
            .activateRecipes(
              "org.openrewrite.java.micronaut.UpdateMicronautPlatformBom",
              "org.openrewrite.java.micronaut.UpdateMicronautValidation"));
    }

    @Language("java")
    private final String annotatedJavaxClass = """
      import jakarta.inject.Singleton;
      import javax.validation.constraints.NotBlank;
                
      @Singleton
      public class PersonService {
          public void sayHello(@NotBlank String name) {
              System.out.println("Hello " + name);
          }
      }
      """;

    @Language("java")
    private final String annotatedJakartaClass = """
      import jakarta.inject.Singleton;
      import jakarta.validation.constraints.NotBlank;
                
      @Singleton
      public class PersonService {
          public void sayHello(@NotBlank String name) {
              System.out.println("Hello " + name);
          }
      }
      """;

    @Language("groovy")
    private final String buildGradleInitial = """
      dependencies {
          annotationProcessor("io.micronaut:micronaut-http-validation")
          implementation("io.micronaut:micronaut-http-client")
          implementation("io.micronaut:micronaut-jackson-databind")
          implementation("io.micronaut:micronaut-validation")
          runtimeOnly("ch.qos.logback:logback-classic")
      }
      """;

    @Language("groovy")
    private final String buildGradleExpected = """
          dependencies {
              annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
              implementation("io.micronaut:micronaut-http-client")
              implementation("io.micronaut:micronaut-jackson-databind")
              implementation("io.micronaut.validation:micronaut-validation")
              runtimeOnly("ch.qos.logback:logback-classic")
          }
      """;

    @Language("xml")
    private final String pomInitial = """
      <project>
          <groupId>com.mycompany.app</groupId>
          <artifactId>my-app</artifactId>
          <version>1</version>
          <parent>
              <groupId>io.micronaut</groupId>
              <artifactId>micronaut-parent</artifactId>
              <version>3.9.1</version>
          </parent>
          <dependencies>
              <dependency>
                  <groupId>io.micronaut</groupId>
                  <artifactId>micronaut-http-client</artifactId>
                  <scope>compile</scope>
              </dependency>
              <dependency>
                  <groupId>io.micronaut</groupId>
                  <artifactId>micronaut-http-server-netty</artifactId>
                  <scope>compile</scope>
              </dependency>
              <dependency>
                  <groupId>io.micronaut</groupId>
                  <artifactId>micronaut-jackson-databind</artifactId>
                  <scope>compile</scope>
              </dependency>
              <dependency>
                  <groupId>io.micronaut</groupId>
                  <artifactId>micronaut-validation</artifactId>
                  <scope>compile</scope>
              </dependency>
              <dependency>
                  <groupId>ch.qos.logback</groupId>
                  <artifactId>logback-classic</artifactId>
                  <scope>runtime</scope>
              </dependency>
          </dependencies>
          <build>
              <plugins>
                  <plugin>
                      <groupId>io.micronaut.build</groupId>
                      <artifactId>micronaut-maven-plugin</artifactId>
                  </plugin>
                  <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <configuration>
                          <annotationProcessorPaths combine.children="append">
                              <path>
                                  <groupId>io.micronaut</groupId>
                                  <artifactId>micronaut-http-validation</artifactId>
                                  <version>${micronaut.version}</version>
                              </path>
                          </annotationProcessorPaths>
                          <compilerArgs>
                              <arg>-Amicronaut.processing.group=com.example</arg>
                              <arg>-Amicronaut.processing.module=demo</arg>
                          </compilerArgs>
                      </configuration>
                  </plugin>
              </plugins>
          </build>
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
                  <artifactId>micronaut-http-client</artifactId>
                  <scope>compile</scope>
              </dependency>
              <dependency>
                  <groupId>io.micronaut</groupId>
                  <artifactId>micronaut-http-server-netty</artifactId>
                  <scope>compile</scope>
              </dependency>
              <dependency>
                  <groupId>io.micronaut</groupId>
                  <artifactId>micronaut-jackson-databind</artifactId>
                  <scope>compile</scope>
              </dependency>
              <dependency>
                  <groupId>io.micronaut.validation</groupId>
                  <artifactId>micronaut-validation</artifactId>
                  <scope>compile</scope>
              </dependency>
              <dependency>
                  <groupId>jakarta.validation</groupId>
                  <artifactId>jakarta.validation-api</artifactId>
              </dependency>
              <dependency>
                  <groupId>ch.qos.logback</groupId>
                  <artifactId>logback-classic</artifactId>
                  <scope>runtime</scope>
              </dependency>
          </dependencies>
          <build>
              <plugins>
                  <plugin>
                      <groupId>io.micronaut.build</groupId>
                      <artifactId>micronaut-maven-plugin</artifactId>
                  </plugin>
                  <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <configuration>
                          <annotationProcessorPaths combine.children="append">
                              <path>
                                  <groupId>io.micronaut.validation</groupId>
                                  <artifactId>micronaut-validation-processor</artifactId>
                                  <version>${micronaut.validation.version}</version>
                                  <exclusions>
                                      <exclusion>
                                          <groupId>io.micronaut</groupId>
                                          <artifactId>micronaut-inject</artifactId>
                                      </exclusion>
                                  </exclusions>
                              </path>
                          </annotationProcessorPaths>
                          <compilerArgs>
                              <arg>-Amicronaut.processing.group=com.example</arg>
                              <arg>-Amicronaut.processing.module=demo</arg>
                          </compilerArgs>
                      </configuration>
                  </plugin>
              </plugins>
          </build>
      </project>
      """;


    @Test
    void updateJavaCodeAndModifyGradleDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
          buildGradle(buildGradleInitial, buildGradleExpected)));
    }

    @Test
    void updateJavaCodeAndModifyMavenDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
          pomXml(pomInitial, pomExpected)));
    }
}
