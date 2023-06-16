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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeMicronautMavenPropertyVersionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeMicronautMavenPropertyVersion("~2.1"));
    }

    @Test
    void changeMavenMicronautVersion() {
        rewriteRun(
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
                          <micronaut.version>2.1.4</micronaut.version>
                      </properties>
                  </project>
              """)
        );
    }

    @Test
    void changeMavenMicronautVersion4() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeMicronautMavenPropertyVersion("4.0.0-M4")),
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
                          <micronaut.version>4.0.0-M4</micronaut.version>
                      </properties>
                  </project>
              """)
        );
    }
}
