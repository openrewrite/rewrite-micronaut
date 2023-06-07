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
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

public class UpdateJakartaPersistenceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jakarta.persistence-api-3.*", "javax.persistence-api-2.*"));
        spec.recipe(RewriteTest.fromRuntimeClasspath("org.openrewrite.java.migrate.jakarta.JavaxPersistenceToJakartaPersistence"));
    }

    @Language("java")
    private final String annotatedJavaxClass = """
            import javax.persistence.Entity;
            import javax.persistence.Id;
            import javax.persistence.GeneratedValue;
            
            @Entity
            public class Person {
                
                @Id
                @GeneratedValue
                private Long id;
            }
        """;

    @Language("java")
    private final String annotatedJakartaClass = """
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.GeneratedValue;
            
            @Entity
            public class Person {
                
                @Id
                @GeneratedValue
                private Long id;
            }
        """;

    @Test
    void updateJavaCodeWithGradleBuild() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
                properties("micronautVersion=3.9.2", s -> s.path("gradle.properties")),
                //language=groovy
                buildGradle("""
                            plugins {
                                id("io.micronaut.application") version "3.7.9"
                            }
                            
                            repositories {
                                mavenCentral()
                            }
                            
                            dependencies {
                                 annotationProcessor("io.micronaut.data:micronaut-data-processor")
                                 annotationProcessor("io.micronaut:micronaut-http-validation")
                                 implementation("io.micronaut:micronaut-http-client")
                                 implementation("io.micronaut:micronaut-jackson-databind")
                                 implementation("io.micronaut.data:micronaut-data-hibernate-jpa")
                                 implementation("io.micronaut.sql:micronaut-jdbc-hikari")
                                 runtimeOnly("ch.qos.logback:logback-classic")
                                 runtimeOnly("com.h2database:h2")
                             }
                        """)));
    }

    @Test
    void updateJavaCodeWithMavenBuild() {
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
                                    <version>3.9.2</version>
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
                                        <groupId>io.micronaut.data</groupId>
                                        <artifactId>micronaut-data-hibernate-jpa</artifactId>
                                        <scope>compile</scope>
                                    </dependency>
                                    <dependency>
                                        <groupId>io.micronaut.sql</groupId>
                                        <artifactId>micronaut-jdbc-hikari</artifactId>
                                        <scope>compile</scope>
                                    </dependency>
                                    <dependency>
                                        <groupId>ch.qos.logback</groupId>
                                        <artifactId>logback-classic</artifactId>
                                        <scope>runtime</scope>
                                    </dependency>
                                    <dependency>
                                        <groupId>com.h2database</groupId>
                                        <artifactId>h2</artifactId>
                                        <scope>runtime</scope>
                                    </dependency>
                                </dependencies>
                            </project>
                        """)));
    }
}
