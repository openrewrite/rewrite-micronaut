package org.openrewrite.java.micronaut;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

public class UpdateBuildToJava17Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.UpdateBuildToJava17");
    }

    @Test
    void updateGradleJavaVersion() {
        rewriteRun(mavenProject("project",
                buildGradle(
                        """
                          version = "0.1.0-SNAPSHOT"
                          group = "com.example"
                          java {
                              sourceCompatibility = JavaVersion.toVersion("1.8")
                              targetCompatibility = JavaVersion.toVersion("1.8")
                          }
                          """,
                        """
                          version = "0.1.0-SNAPSHOT"
                          group = "com.example"
                          java {
                              sourceCompatibility = JavaVersion.toVersion("17")
                              targetCompatibility = JavaVersion.toVersion("17")
                          }
                          """
                ))
        );
    }

    @Test
    void updateMavenJavaVersion() {
        rewriteRun(mavenProject("project",
                pomXml(
                        """
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
                                """,
                        """
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
                          """
                ))
        );
    }
}
