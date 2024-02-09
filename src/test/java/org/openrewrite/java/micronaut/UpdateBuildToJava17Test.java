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
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class UpdateBuildToJava17Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate").build().activateRecipes("org.openrewrite.java.migrate.JavaVersion17"));
    }

    @Test
    void updateGradleJavaVersion() {
        rewriteRun(mavenProject("project",
          //language=groovy
          buildGradle("""
            version = "0.1.0-SNAPSHOT"
            group = "com.example"
            java {
                sourceCompatibility = JavaVersion.toVersion("1.8")
                targetCompatibility = JavaVersion.toVersion("1.8")
            }
            """, """
            version = "0.1.0-SNAPSHOT"
            group = "com.example"
            java {
                sourceCompatibility = JavaVersion.toVersion("17")
                targetCompatibility = JavaVersion.toVersion("17")
            }
            """)));
    }

    @Test
    void updateMavenJavaVersion() {
        rewriteRun(mavenProject("project",
          //language=xml
          pomXml("""
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <packaging>jar</packaging>
                      <jdk.version>1.8</jdk.version>
                      <release.version>8</release.version>
                  </properties>
              </project>
          """, """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <properties>
                            <packaging>jar</packaging>
                            <jdk.version>17</jdk.version>
                            <release.version>17</release.version>
                        </properties>
                    </project>
          """)));
    }
}
