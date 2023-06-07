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

public class UpdateMicronautEmailTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "micronaut-email-2.0.0-M1", "jakarta.inject-api-2.*", "jakarta.mail-api-2.*", "javax.mail-api-1.*"));
        spec.recipeFromResource("/META-INF/rewrite/micronaut3-to-4.yml", "org.openrewrite.java.micronaut.UpdateMicronautEmail");
    }

    @Language("java")
    private final String javaxMailClass = """
            import io.micronaut.email.EmailSender;
            import jakarta.inject.Singleton;
            import javax.mail.Message;
            
            @Singleton
            public class CustomizedJavaMailService {
            
                private final EmailSender<Message, ?> emailSender;
                
                public CustomizedJavaMailService(EmailSender<Message, ?> emailSender) {
                    this.emailSender = emailSender;
                }        
            }
        """;

    @Language("java")
    private final String jakartaMailClass = """
            import io.micronaut.email.EmailSender;
            import jakarta.inject.Singleton;
            import jakarta.mail.Message;
            
            @Singleton
            public class CustomizedJavaMailService {
            
                private final EmailSender<Message, ?> emailSender;
                
                public CustomizedJavaMailService(EmailSender<Message, ?> emailSender) {
                    this.emailSender = emailSender;
                }        
            }
        """;

    @Test
    void updateJavaCode() {
        rewriteRun(mavenProject("project", srcMainJava(java(javaxMailClass, jakartaMailClass))));
    }
}
