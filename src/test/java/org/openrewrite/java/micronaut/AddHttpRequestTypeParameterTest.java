package org.openrewrite.java.micronaut;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

public class AddHttpRequestTypeParameterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "micronaut-security-4.*",
          "micronaut-http-4.*"));
        spec.recipe(new AddHttpRequestTypeParameter());
    }

    @Test
    void testAuthenticationProvider() {
        rewriteRun(mavenProject("project", srcMainJava(
          //language=java
          java("""
          import io.micronaut.http.HttpRequest;
          import io.micronaut.security.authentication.AuthenticationProvider;
          import io.micronaut.security.authentication.AuthenticationRequest;
          import org.reactivestreams.Publisher;
          
          public class AuthenticationProviderUserPassword implements AuthenticationProvider {
          
              @Override
              public Publisher<AuthenticationResponse> authenticate(HttpRequest<?> httpRequest,
                                                                    AuthenticationRequest<?, ?> authenticationRequest) {
                  return null;
              }
          }
          """, """
          import io.micronaut.http.HttpRequest;
          import io.micronaut.security.authentication.AuthenticationProvider;
          import io.micronaut.security.authentication.AuthenticationRequest;
          import org.reactivestreams.Publisher;
          
          public class AuthenticationProviderUserPassword implements AuthenticationProvider<HttpRequest<?>> {
          
              @Override
              public Publisher<AuthenticationResponse> authenticate(HttpRequest<?> httpRequest,
                                                                    AuthenticationRequest<?, ?> authenticationRequest) {
                  return null;
              }
          }
          """))));
    }
}
