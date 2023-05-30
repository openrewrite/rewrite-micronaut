package org.openrewrite.java.micronaut;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

public class UpdateBuildToMicronaut4VersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("micronaut-context"));
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.UpdateBuildToMicronaut4Version");
    }

    @Language("properties")
    private final String initialGradleProperties = """
            micronautVersion=3.9.0    
        """;

    @Language("properties")
    private final String v4GradleProperties = """
            micronautVersion=4.0.0-M2    
        """;

    @Language("xml")
    private final String initialPomXml = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                    <micronaut.version>3.9.0</micronaut.version>
                </properties>
            </project>
        """;

    @Language("xml")
    private final String v4PomXml = """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                    <micronaut.version>4.0.0-M2</micronaut.version>
                </properties>
            </project>
        """;

    @Test
    public void updateGradleProperties() {
        rewriteRun(properties(initialGradleProperties, v4GradleProperties,
                s -> s.path("gradle.properties")));
    }

    @Test
    public void updatePomXmlProperties() {
        rewriteRun(pomXml(initialPomXml, v4PomXml));
    }
}
