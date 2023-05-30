package org.openrewrite.java.micronaut;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

public class UpdateMicronautEmailTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("micronaut-email", "jakarta.inject", "jakarta.mail-api", "javax.mail-api"));
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
