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

public class AddMicronautWebsocketDependencyIfNeededTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("micronaut-websocket"));
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.AddMicronautWebsocketDependencyIfNeeded");
    }

    @Language("java")
    private final String annotatedWebsocketClass = """
            import io.micronaut.websocket.WebSocketBroadcaster;
            import io.micronaut.websocket.annotation.ServerWebSocket;       
            
            @ServerWebSocket("/chat/{topic}/{username}")
            public class ChatServerWebSocket {
            
                private final WebSocketBroadcaster broadcaster;
                
                public ChatServerWebSocket(WebSocketBroadcaster broadcaster) {
                    this.broadcaster = broadcaster;
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
                implementation "io.micronaut:micronaut-websocket"
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
                        <artifactId>micronaut-websocket</artifactId>
                    </dependency>
                </dependencies>
            </project>
        """;

    @Test
    void updateGradleDependencies() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()), mavenProject("project", srcMainJava(java(annotatedWebsocketClass)), buildGradle(buildGradleInitial, buildGradleExpected)));
    }

    @Test
    void updateMavenDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedWebsocketClass)), pomXml(pomInitial, pomExpected)));
    }
}
