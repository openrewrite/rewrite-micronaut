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
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

public class UpdateJakartaAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jakarta.inject-api-2.*", "jakarta.annotation-api-2.*", "javax.annotation-api-1.3.2"));
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.micronaut")
          .build()
          .activateRecipes("org.openrewrite.java.micronaut.UpdateJakartaAnnotations"));
    }

    @Language("java")
    private final String annotatedJavaxClass = """
            import javax.annotation.PostConstruct;
            import jakarta.inject.Singleton;
            
            @Singleton
            public class FooService {
            
                @PostConstruct
                public void init() {
                
                }
            }
        """;

    @Language("java")
    private final String annotatedJakartaClass = """
            import jakarta.annotation.PostConstruct;
            import jakarta.inject.Singleton;
            
            @Singleton
            public class FooService {
            
                @PostConstruct
                public void init() {
                
                }
            }
        """;

    private final SourceSpecs gradleProperties = properties("micronautVersion=4.0.0-M3", s -> s.path("gradle.properties"));

    @Language("groovy")
    private final String buildGradleWithDependency = """
            plugins {
                id("io.micronaut.application") version "4.0.0-M4"
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                annotationProcessor("io.micronaut:micronaut-http-validation")
                implementation("io.micronaut:micronaut-http-client")
                implementation("io.micronaut:micronaut-jackson-databind")
                implementation "jakarta.annotation:jakarta.annotation-api:2.1.1"
                runtimeOnly("ch.qos.logback:logback-classic")
            }
        """;

    @Language("groovy")
    private final String buildGradleWithoutDependency = """
            plugins {
                id("io.micronaut.application") version "4.0.0-M4"
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
                    <version>4.0.0-M3</version>
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
                        <groupId>jakarta.annotation</groupId>
                        <artifactId>jakarta.annotation-api</artifactId>
                        <scope>compile</scope>
                    </dependency>
                    <dependency>
                        <groupId>ch.qos.logback</groupId>
                        <artifactId>logback-classic</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
            </project>
        """;

    @Language("xml")
    private final String pomWithoutDependency = """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>io.micronaut.platform</groupId>
                    <artifactId>micronaut-parent</artifactId>
                    <version>4.0.0-M3</version>
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
            </project>
        """;

    @Test
    void updateJavaCodeAndRemoveGradleDependency() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()), mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
                gradleProperties, buildGradle(buildGradleWithDependency, buildGradleWithoutDependency)));

    }

    @Test
    void updateJavaCodeAndRemoveMavenDependency() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
                pomXml(pomWithDependency, pomWithoutDependency)));
    }
}
