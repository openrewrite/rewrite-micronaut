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
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class AddMicronautWebsocketDependencyIfNeededTest extends Micronaut4RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "micronaut-websocket-4.*"));
        spec.recipes(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.micronaut")
          .build()
          .activateRecipes("org.openrewrite.java.micronaut.AddMicronautWebsocketDependencyIfNeeded"));
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

    @Test
    void updateGradleDependencies() {
        rewriteRun(spec -> spec.beforeRecipe(withToolingApi()), mavenProject("project", srcMainJava(java(annotatedWebsocketClass)), getGradleProperties(),
          //language=groovy
          buildGradle("""
                plugins {
                    id("io.micronaut.application") version "%s"
                }
                
                repositories {
                    mavenCentral()
                }
            """.formatted(latestApplicationPluginVersion), """
                plugins {
                    id("io.micronaut.application") version "%s"
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "io.micronaut:micronaut-websocket"
                }
            """.formatted(latestApplicationPluginVersion))));
    }

    @Test
    void updateMavenDependencies() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedWebsocketClass)),
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
                </project>
            """.formatted(latestMicronautVersion), """
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
                            <artifactId>micronaut-websocket</artifactId>
                        </dependency>
                    </dependencies>
                </project>
            """.formatted(latestMicronautVersion))));
    }
}
