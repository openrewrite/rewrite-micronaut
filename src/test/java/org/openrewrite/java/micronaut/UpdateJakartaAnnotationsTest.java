package org.openrewrite.java.micronaut;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

public class UpdateJakartaAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("jakarta.inject-api", "jakarta.annotation-api", "javax.annotation-api"));
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.UpdateJakartaAnnotations");
    }

    @Language("java")
    private final String annotatedJavaxClass = """
            import jakarta.inject.Singleton;
            import javax.annotation.PostConstruct;
            
            @Singleton
            public class FooService {
            
                @PostConstruct
                public void init() {
                
                }
            }
        """;

    @Language("java")
    private final String annotatedJakartaClass = """
            import jakarta.inject.Singleton;
            import jakarta.annotation.PostConstruct;
            
            @Singleton
            public class FooService {
            
                @PostConstruct
                public void init() {
                
                }
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
                id("io.micronaut.application") version "4.0.0-M2"
            }
            
            micronaut { version '4.0.0-M2'}
            
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
                    <version>4.0.0-M2</version>
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
                    <version>4.0.0-M2</version>
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
                buildGradle(buildGradleWithDependency, buildGradleWithoutDependency)));

    }

    @Test
    void updateJavaCodeAndRemoveMavenDependency() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass)),
                pomXml(pomWithDependency, pomWithoutDependency)));
    }
}
