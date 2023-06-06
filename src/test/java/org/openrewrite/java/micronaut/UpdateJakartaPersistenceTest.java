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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

public class UpdateJakartaPersistenceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jakarta.persistence-api-3.*", "javax.persistence-api-2.*"));
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
