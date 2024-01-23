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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

class AddHttpRequestTypeParameterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
          "micronaut-security-4.*",
          "micronaut-security-jwt-4.*",
          "micronaut-security-oauth2-4.*",
          "micronaut-http-4.*"));
        spec.recipe(new AddHttpRequestTypeParameter());
        spec.expectedCyclesThatMakeChanges(2);
    }

    @Test
    void testAuthenticationProviderNoChangesNeeded() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(0),
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.authentication.AuthenticationProvider;
            import io.micronaut.security.authentication.AuthenticationRequest;
            import io.micronaut.security.authentication.AuthenticationResponse;
            import org.reactivestreams.Publisher;
                      
            public class AuthenticationProviderUserPassword implements AuthenticationProvider<HttpRequest<?>> {
                      
                @Override
                public Publisher<AuthenticationResponse> authenticate(HttpRequest<?> httpRequest,
                                                                      AuthenticationRequest<?, ?> authenticationRequest) {
                    return null;
                }
            }
            """));
    }

    @Test
    void testAuthenticationProvider() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.authentication.AuthenticationProvider;
            import io.micronaut.security.authentication.AuthenticationRequest;
            import io.micronaut.security.authentication.AuthenticationResponse;
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
            import io.micronaut.security.authentication.AuthenticationResponse;
            import org.reactivestreams.Publisher;
                      
            public class AuthenticationProviderUserPassword implements AuthenticationProvider<HttpRequest<?>> {
                      
                @Override
                public Publisher<AuthenticationResponse> authenticate(HttpRequest<?> httpRequest,
                                                                      AuthenticationRequest<?, ?> authenticationRequest) {
                    return null;
                }
            }
            """));
    }

    @Test
    void testAuthenticationProviderWithAdditionalInterface() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.authentication.AuthenticationProvider;
            import io.micronaut.security.authentication.AuthenticationRequest;
            import io.micronaut.security.authentication.AuthenticationResponse;
            import org.reactivestreams.Publisher;
                        
            import java.io.Serializable;
                      
            public class AuthenticationProviderUserPassword implements AuthenticationProvider, Serializable {
                      
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
            import io.micronaut.security.authentication.AuthenticationResponse;
            import org.reactivestreams.Publisher;
                      
            import java.io.Serializable;
                      
            public class AuthenticationProviderUserPassword implements AuthenticationProvider<HttpRequest<?>>, Serializable {
                      
                @Override
                public Publisher<AuthenticationResponse> authenticate(HttpRequest<?> httpRequest,
                                                                      AuthenticationRequest<?, ?> authenticationRequest) {
                    return null;
                }
            }
            """));
    }

    @Test
    void testGenericJwtClaimsValidator() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.token.Claims;
            import io.micronaut.security.token.jwt.validator.GenericJwtClaimsValidator;
                        
            public class CustomHttpRequestImpl implements GenericJwtClaimsValidator {
                @Override
                public boolean validate(Claims claims, HttpRequest<?> request) {
                    return false;
                }
            }
            """, """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.token.Claims;
            import io.micronaut.security.token.jwt.validator.GenericJwtClaimsValidator;
                        
            public class CustomHttpRequestImpl implements GenericJwtClaimsValidator<HttpRequest<?>> {
                @Override
                public boolean validate(Claims claims, HttpRequest<?> request) {
                    return false;
                }
            }
            """));
    }

    @Test
    void testJwtClaimsValidator() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.token.Claims;
            import io.micronaut.security.token.jwt.validator.JwtClaimsValidator;
                        
            public class CustomHttpRequestImpl implements JwtClaimsValidator {
                @Override
                public boolean validate(Claims claims, HttpRequest<?> request) {
                    return false;
                }
            }
            """, """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.token.Claims;
            import io.micronaut.security.token.jwt.validator.JwtClaimsValidator;
                        
            public class CustomHttpRequestImpl implements JwtClaimsValidator<HttpRequest<?>> {
                @Override
                public boolean validate(Claims claims, HttpRequest<?> request) {
                    return false;
                }
            }
            """));
    }

    @Test
    void testEndSessionCallbackUrlBuilder() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.oauth2.endpoint.endsession.response.EndSessionCallbackUrlBuilder;
            
            import java.net.URL;
            
            public class CustomHttpRequestImpl implements EndSessionCallbackUrlBuilder {
                @Override
                public URL build(HttpRequest<?> originating) {
                    return null;
                }
            }
            """, """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.oauth2.endpoint.endsession.response.EndSessionCallbackUrlBuilder;
            
            import java.net.URL;
            
            public class CustomHttpRequestImpl implements EndSessionCallbackUrlBuilder<HttpRequest<?>> {
                @Override
                public URL build(HttpRequest<?> originating) {
                    return null;
                }
            }
            """));
    }

    @Test
    void testAbsoluteUrlBuilder() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.oauth2.url.AbsoluteUrlBuilder;
            
            import java.net.URL;
            
            public class CustomHttpRequestImpl implements AbsoluteUrlBuilder {
                @Override
                public URL buildUrl(HttpRequest<?> current, String path) {
                    return null;
                }
            }
            """, """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.oauth2.url.AbsoluteUrlBuilder;
            
            import java.net.URL;
            
            public class CustomHttpRequestImpl implements AbsoluteUrlBuilder<HttpRequest<?>> {
                @Override
                public URL buildUrl(HttpRequest<?> current, String path) {
                    return null;
                }
            }
            """));
    }

    @Test
    void testOauthRouteUrlBuilder() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.oauth2.url.OauthRouteUrlBuilder;
            
            import java.net.URI;
            import java.net.URL;
            
            public class CustomHttpRequestImpl implements OauthRouteUrlBuilder {
            
                @Override
                public URL buildLoginUrl(HttpRequest<?> originating, String providerName) {
                    return null;
                }
            
                @Override
                public URL buildCallbackUrl(HttpRequest<?> originating, String providerName) {
                    return null;
                }
            
                @Override
                public URI buildLoginUri(String providerName) {
                    return null;
                }
            
                @Override
                public URI buildCallbackUri(String providerName) {
                    return null;
                }
            
                @Override
                public URL buildUrl(HttpRequest<?> current, String path) {
                    return null;
                }
            }
            """, """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.oauth2.url.OauthRouteUrlBuilder;
            
            import java.net.URI;
            import java.net.URL;
            
            public class CustomHttpRequestImpl implements OauthRouteUrlBuilder<HttpRequest<?>> {
            
                @Override
                public URL buildLoginUrl(HttpRequest<?> originating, String providerName) {
                    return null;
                }
            
                @Override
                public URL buildCallbackUrl(HttpRequest<?> originating, String providerName) {
                    return null;
                }
            
                @Override
                public URI buildLoginUri(String providerName) {
                    return null;
                }
            
                @Override
                public URI buildCallbackUri(String providerName) {
                    return null;
                }
            
                @Override
                public URL buildUrl(HttpRequest<?> current, String path) {
                    return null;
                }
            }
            """));
    }

    @Test
    void testIntrospectionProcessor() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.authentication.Authentication;
            import io.micronaut.security.endpoints.introspection.IntrospectionProcessor;
            import io.micronaut.security.endpoints.introspection.IntrospectionRequest;
            import io.micronaut.security.endpoints.introspection.IntrospectionResponse;
            import org.reactivestreams.Publisher;
            
            public class CustomHttpRequestImpl implements IntrospectionProcessor {
            
                @Override
                public Publisher<IntrospectionResponse> introspect(IntrospectionRequest introspectionRequest, HttpRequest<?> httpRequest) {
                    return null;
                }
            
                @Override
                public Publisher<IntrospectionResponse> introspect(Authentication authentication, HttpRequest<?> httpRequest) {
                    return null;
                }
            }
            """, """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.authentication.Authentication;
            import io.micronaut.security.endpoints.introspection.IntrospectionProcessor;
            import io.micronaut.security.endpoints.introspection.IntrospectionRequest;
            import io.micronaut.security.endpoints.introspection.IntrospectionResponse;
            import org.reactivestreams.Publisher;
            
            public class CustomHttpRequestImpl implements IntrospectionProcessor<HttpRequest<?>> {
            
                @Override
                public Publisher<IntrospectionResponse> introspect(IntrospectionRequest introspectionRequest, HttpRequest<?> httpRequest) {
                    return null;
                }
            
                @Override
                public Publisher<IntrospectionResponse> introspect(Authentication authentication, HttpRequest<?> httpRequest) {
                    return null;
                }
            }
            """));
    }

    @Test
    void testAuthenticationFetcher() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.authentication.Authentication;
            import io.micronaut.security.filters.AuthenticationFetcher;
            import org.reactivestreams.Publisher;
            
            public class CustomHttpRequestImpl implements AuthenticationFetcher {
                @Override
                public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
                    return null;
                }
            }
            """, """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.authentication.Authentication;
            import io.micronaut.security.filters.AuthenticationFetcher;
            import org.reactivestreams.Publisher;
             
            public class CustomHttpRequestImpl implements AuthenticationFetcher<HttpRequest<?>> {
                @Override
                public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
                    return null;
                }
            }
            """));
    }

    @Test
    void testTokenReader() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.token.reader.TokenReader;
            
            import java.util.Optional;
            
            public class CustomHttpRequestImpl implements TokenReader {
                @Override
                public Optional<String> findToken(HttpRequest<?> request) {
                    return Optional.empty();
                }
            }
            """, """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.token.reader.TokenReader;
            
            import java.util.Optional;
            
            public class CustomHttpRequestImpl implements TokenReader<HttpRequest<?>> {
                @Override
                public Optional<String> findToken(HttpRequest<?> request) {
                    return Optional.empty();
                }
            }
            """));
    }

    @Test
    void testTokenResolver() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.token.reader.TokenResolver;
            
            import java.util.Optional;
            
            public class CustomHttpRequestImpl implements TokenResolver {
                @Override
                public Optional<String> resolveToken(HttpRequest<?> request) {
                    return Optional.empty();
                }
            }
            """, """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.token.reader.TokenResolver;
            
            import java.util.Optional;
            
            public class CustomHttpRequestImpl implements TokenResolver<HttpRequest<?>> {
                @Override
                public Optional<String> resolveToken(HttpRequest<?> request) {
                    return Optional.empty();
                }
            }
            """));
    }

    @Test
    void testTokenValidator() {
        rewriteRun(
          //language=java
          java(
                """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.authentication.Authentication;
            import io.micronaut.security.token.validator.TokenValidator;
            import org.reactivestreams.Publisher;
            
            public class CustomHttpRequestImpl implements TokenValidator {
                @Override
                public Publisher<Authentication> validateToken(String token, HttpRequest<?> request) {
                    return null;
                }
            }
            """, """
            import io.micronaut.http.HttpRequest;
            import io.micronaut.security.authentication.Authentication;
            import io.micronaut.security.token.validator.TokenValidator;
            import org.reactivestreams.Publisher;
            
            public class CustomHttpRequestImpl implements TokenValidator<HttpRequest<?>> {
                @Override
                public Publisher<Authentication> validateToken(String token, HttpRequest<?> request) {
                    return null;
                }
            }
            """));
    }
}
