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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeMicronautMavenPropertyVersionTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeMavenMicronautVersion() {
        String latestMicronautVersion = MicronautVersionHelper.getLatestMN2Version();

        rewriteRun(spec -> spec.recipe(new UpgradeMicronautMavenPropertyVersion("2.x")),
          pomXml("""
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <micronaut.version>2.0.3</micronaut.version>
                      </properties>
                  </project>
              """,
                  """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <micronaut.version>%s</micronaut.version>
                      </properties>
                  </project>
              """.formatted(latestMicronautVersion))
        );
    }

    @Test
    void changeMavenMicronautVersion3() {
        String latestMicronautVersion = MicronautVersionHelper.getLatestMN3Version();

        rewriteRun(spec -> spec.recipe(new UpgradeMicronautMavenPropertyVersion("3.x")),
          pomXml("""
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <micronaut.version>2.0.3</micronaut.version>
                      </properties>
                  </project>
              """,
                  """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <micronaut.version>%s</micronaut.version>
                      </properties>
                  </project>
              """.formatted(latestMicronautVersion))
        );
    }

    @Test
    void changeMavenMicronautVersion4() {
        String latestMicronautVersion = MicronautVersionHelper.getLatestMN4Version();

        rewriteRun(
          spec -> spec.recipe(new UpgradeMicronautMavenPropertyVersion("4.x")),
          pomXml("""
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <micronaut.version>3.9.1</micronaut.version>
                      </properties>
                  </project>
              """,
                  """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <micronaut.version>%s</micronaut.version>
                      </properties>
                  </project>
              """.formatted(latestMicronautVersion))
        );
    }
}
