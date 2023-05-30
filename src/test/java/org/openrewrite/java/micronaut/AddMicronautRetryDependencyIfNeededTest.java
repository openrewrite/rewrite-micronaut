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

public class AddMicronautRetryDependencyIfNeededTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("jakarta.inject-api", "micronaut-retry"));
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.AddMicronautRetryDependencyIfNeeded");
    }

    @Language("java")
    private final String retryableService = """
            import jakarta.inject.Singleton;
            import io.micronaut.retry.annotation.Retryable;
            
            @Singleton
            public class PersonService {
            
                @Retryable
                public void callFlakyRemote() {
                    
                }
            }
        """;

    @Language("groovy")
    private final String buildGradleInitial = """
            plugins {
                id("io.micronaut.application") version "4.0.0-M2"
            }
            
            micronaut { version '4.0.0-M2'}
            
            repositories {
                mavenCentral()
            }
        """;

    @Language("groovy")
    private final String buildGradleExpected = """
            plugins {
                id("io.micronaut.application") version "4.0.0-M2"
            }
            
            micronaut { version '4.0.0-M2'}
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation "io.micronaut:micronaut-retry"
            }
        """;

    @Language("xml")
    private final String pomInitial = """
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
    private final String pomExpected = """
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
                        <artifactId>micronaut-retry</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """;

    @Test
    void updateGradleDependencies() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()), mavenProject("project", srcMainJava(java(retryableService)), buildGradle(buildGradleInitial, buildGradleExpected)));
    }

    @Test
    void updateMavenDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(retryableService)), pomXml(pomInitial, pomExpected)));
    }
}
