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

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

public class AddTestResourcesClientDependencyIfNeededTest extends Micronaut4RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "micronaut-context-4.*"))
          .recipe(new AddTestResourcesClientDependencyIfNeeded());
    }

    @Language("java")
    private final String micronautApplication = """
            import io.micronaut.runtime.Micronaut;
            
            public class Application {
            
                public static void main(String[] args) {
                    Micronaut.run(Application.class, args);
                }
            }
      """;

    @Test
    void addTestResourcesClientDependencyForMaven() {
        rewriteRun(mavenProject("project", srcMainJava(java(micronautApplication)),
          //language=xml
          pomXml("""
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <parent>
                      <groupId>io.micronaut.platform</groupId>
                      <artifactId>micronaut-parent</artifactId>
                      <version>%1$s</version>
                  </parent>
                  <properties>
                      <micronaut.version>%1$s</micronaut.version>
                      <micronaut.test.resources.enabled>true</micronaut.test.resources.enabled>
                  </properties>
              </project>
              """.formatted(latestMicronautVersion),
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <parent>
                      <groupId>io.micronaut.platform</groupId>
                      <artifactId>micronaut-parent</artifactId>
                      <version>%1$s</version>
                  </parent>
                  <properties>
                      <micronaut.version>%1$s</micronaut.version>
                      <micronaut.test.resources.enabled>true</micronaut.test.resources.enabled>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>io.micronaut.testresources</groupId>
                          <artifactId>micronaut-test-resources-client</artifactId>
                          <scope>provided</scope>
                      </dependency>
                  </dependencies>
              </project>
              """.formatted(latestMicronautVersion))));
    }

    @Test
    void noDependencyAddedForPropertyFalse() {
        rewriteRun(mavenProject("project", srcMainJava(java(micronautApplication)),
          //language=xml
          pomXml("""
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>io.micronaut.platform</groupId>
                    <artifactId>micronaut-parent</artifactId>
                    <version>%1$s</version>
                </parent>
                <properties>
                    <micronaut.version>%1$s</micronaut.version>
                    <micronaut.test.resources.enabled>false</micronaut.test.resources.enabled>
                </properties>
            </project>
            """.formatted(latestMicronautVersion))));
    }

    @Test
    void noDependencyAddedForPropertyNotSet() {
        rewriteRun(mavenProject("project", srcMainJava(java(micronautApplication)),
          //language=xml
          pomXml("""
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <parent>
                    <groupId>io.micronaut.platform</groupId>
                    <artifactId>micronaut-parent</artifactId>
                    <version>%1$s</version>
                </parent>
                <properties>
                    <micronaut.version>%1$s</micronaut.version>
                </properties>
            </project>
            """.formatted(latestMicronautVersion))));
    }

}
