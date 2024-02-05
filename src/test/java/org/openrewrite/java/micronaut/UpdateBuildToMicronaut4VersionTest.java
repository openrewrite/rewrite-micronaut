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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

public class UpdateBuildToMicronaut4VersionTest extends Micronaut4RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "micronaut-context-4.*"));
        spec.recipes(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.micronaut")
          .build()
          .activateRecipes("org.openrewrite.java.micronaut.UpdateBuildToMicronaut4Version"));
    }

    @Test
    void updateGradleProperties() {
        rewriteRun(properties("""
              micronautVersion=%s
          """.formatted(MicronautRewriteTestVersions.getLatestMN3Version()), """
              micronautVersion=%s
          """.formatted(latestMicronautVersion), s -> s.path("gradle.properties")));
    }

    @Test
    void updatePomXmlProperties() {
        rewriteRun(
          //language=xml
          pomXml("""
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <micronaut.version>%s</micronaut.version>
                    </properties>
                </project>
            """.formatted(MicronautRewriteTestVersions.getLatestMN3Version()), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <micronaut.version>%s</micronaut.version>
                    </properties>
                </project>
            """.formatted(latestMicronautVersion)));
    }
}
