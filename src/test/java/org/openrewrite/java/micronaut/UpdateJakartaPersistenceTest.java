package org.openrewrite.java.micronaut;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

public class UpdateJakartaPersistenceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("jakarta.persistence-api", "javax.persistence-api"));
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.UpdateJakartaPersistence");
    }

    @Language("java")
    private final String annotatedJavaxClass = """
            import javax.persistence.Entity;
            import javax.persistence.Id;
            import javax.persistence.GeneratedValue;
            
            @Entity
            public class Person {
                
                @Id
                @GeneratedValue
                private Long id;
            }
        """;

    @Language("java")
    private final String annotatedJakartaClass = """
            import jakarta.persistence.Entity;
            import jakarta.persistence.Id;
            import jakarta.persistence.GeneratedValue;
            
            @Entity
            public class Person {
                
                @Id
                @GeneratedValue
                private Long id;
            }
        """;

    @Test
    void updateJavaCode() {
        rewriteRun(mavenProject("project", srcMainJava(java(annotatedJavaxClass, annotatedJakartaClass))));
    }
}
