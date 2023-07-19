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

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

class UpdateMicronautValidationTest extends Micronaut4RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "validation-api-2.*", "jakarta.validation-api-3.*", "jakarta.inject-api-2.*")).recipes(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.micronaut").build().activateRecipes("org.openrewrite.java.micronaut.UpdateMicronautPlatformBom", "org.openrewrite.java.micronaut.UpdateBuildPlugins", "org.openrewrite.java.micronaut.UpdateMicronautValidation"));
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

    @Test
    void updateJavaCodeAndModifyGradleDependencies() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()), mavenProject("project", properties("micronautVersion=" + MicronautRewriteTestVersions.getLatestMN3Version(), s -> s.path("gradle.properties")), srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
          //language=groovy
          buildGradle("""
            plugins {
                id("io.micronaut.application") version "3.7.10"
            }
                          
            repositories {
                mavenCentral()
            }
                          
            dependencies {
                annotationProcessor("io.micronaut:micronaut-http-validation")
                implementation("io.micronaut:micronaut-http-client")
                implementation("io.micronaut:micronaut-jackson-databind")
                implementation("io.micronaut:micronaut-validation")
                runtimeOnly("ch.qos.logback:logback-classic")
            }
            """, """
            plugins {
                id("io.micronaut.application") version "%s"
            }
                          
            repositories {
                mavenCentral()
            }
                          
            dependencies {
                annotationProcessor("io.micronaut:micronaut-http-validation")
                annotationProcessor "io.micronaut.validation:micronaut-validation-processor"
                implementation("io.micronaut:micronaut-http-client")
                implementation("io.micronaut:micronaut-jackson-databind")
                implementation "io.micronaut.validation:micronaut-validation"
                runtimeOnly("ch.qos.logback:logback-classic")
            }
            """.formatted(latestApplicationPluginVersion))));
    }

    @Test
    void updateJavaCodeAndAddMissingGradleDependencies() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()), mavenProject("project", properties("micronautVersion=" + MicronautRewriteTestVersions.getLatestMN3Version(), s -> s.path("gradle.properties")), srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
          //language=groovy
          buildGradle("""
            plugins {
                id("io.micronaut.application") version "3.7.10"
            }
                          
            repositories {
                mavenCentral()
            }
                          
            dependencies {
                annotationProcessor("io.micronaut:micronaut-http-validation")
                implementation("io.micronaut:micronaut-http-client")
                implementation("io.micronaut:micronaut-jackson-databind")
                runtimeOnly("ch.qos.logback:logback-classic")
            }
            """, """
            plugins {
                id("io.micronaut.application") version "%s"
            }
                          
            repositories {
                mavenCentral()
            }
                          
            dependencies {
                annotationProcessor("io.micronaut:micronaut-http-validation")
                annotationProcessor "io.micronaut.validation:micronaut-validation-processor"
                implementation("io.micronaut:micronaut-http-client")
                implementation("io.micronaut:micronaut-jackson-databind")
                implementation "io.micronaut.validation:micronaut-validation"
                runtimeOnly("ch.qos.logback:logback-classic")
            }
            """.formatted(latestApplicationPluginVersion))));
    }

    @Test
    void updateJavaCodeAndModifyMavenDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
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
                            <groupId>io.micronaut.maven</groupId>
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
            """.formatted(latestMicronautVersion))));
    }

    @Test
    void updateJavaCodeAndModifyMavenDependenciesAndAnnotationProcessor() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
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
                                    <path>
                                        <groupId>io.micronaut</groupId>
                                        <artifactId>micronaut-validation</artifactId>
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
                            <groupId>io.micronaut.maven</groupId>
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
            """.formatted(latestMicronautVersion))));
    }

    @Test
    void updateJavaCodeAndAddMissingMavenDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
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
                            <groupId>io.micronaut.maven</groupId>
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
            """.formatted(latestMicronautVersion))));
    }

    @Test
    void updateJavaCodeAndLeaveExistingMavenAnnotationProcessor() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass))),
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
                        <groupId>ch.qos.logback</groupId>
                        <artifactId>logback-classic</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
                <build>
                    <plugins>
                        <plugin>
                            <groupId>io.micronaut.maven</groupId>
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
            """.formatted(latestMicronautVersion)));
    }
}
