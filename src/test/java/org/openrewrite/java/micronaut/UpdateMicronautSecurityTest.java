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
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainResources;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.yaml.Assertions.yaml;

public class UpdateMicronautSecurityTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.micronaut")
          .build()
          .activateRecipes("org.openrewrite.java.micronaut.UpdateMicronautSecurity"));
    }

    @Language("properties")
    private final String initialSecurityProps = """
            micronaut.security.token.jwt.generator.access-token.expiration=1d
            micronaut.security.token.jwt.cookie.enabled=true
            micronaut.security.token.jwt.cookie.cookie-max-age=1d
            micronaut.security.token.jwt.cookie.cookie-path=foo
            micronaut.security.token.jwt.cookie.cookie-domain=bar.com
            micronaut.security.token.jwt.cookie.cookie-same-site=true
            micronaut.security.token.jwt.bearer.enabled=true
        """;

    @Language("properties")
    private final String expectedSecurityProps = """
            micronaut.security.token.generator.access-token.expiration=1d
            micronaut.security.token.cookie.enabled=true
            micronaut.security.token.cookie.cookie-max-age=1d
            micronaut.security.token.cookie.cookie-path=foo
            micronaut.security.token.cookie.cookie-domain=bar.com
            micronaut.security.token.cookie.cookie-same-site=true
            micronaut.security.token.bearer.enabled=true
        """;

    @Language("yml")
    private final String initialSecurityYaml = """
            micronaut:
                security:
                    token:
                        jwt:
                            generator:
                                access-token:
                                    expiration: 1d
                            cookie:
                                enabled: true
                                cookie-max-age: 1d
                                cookie-path: foo
                                cookie-domain: bar.com
                                cookie-same-site: true
                            bearer:
                                enabled: true
        """;

    @Language("yml")
    private final String expectedSecurityYaml = """
            micronaut:
                security:
                    token:
                        generator:
                            access-token:
                                expiration: 1d
                        cookie:
                            enabled: true
                            cookie-max-age: 1d
                            cookie-path: foo
                            cookie-domain: bar.com
                            cookie-same-site: true
                        bearer:
                            enabled: true
        """;

    @Language("yml")
    private final String initialSecurityYamlPartial = """
            micronaut:
                security:
                    token:
                        jwt:
                            generator:
                                access-token:
                                    expiration: 1d
                            cookie:
                                enabled: false
                            bearer:
                                enabled: true
        """;

    @Language("yml")
    private final String expectedSecurityYamlPartial = """
            micronaut:
                security:
                    token:
                        generator:
                            access-token:
                                expiration: 1d
                        cookie:
                            enabled: false
                        bearer:
                            enabled: true
        """;

    @Language("yml")
    private final String noJwtConfig = """
            micronaut:
                application:
                    name: foo
        """;



    @Test
    void updatePropertyConfig() {
        rewriteRun(mavenProject("project", srcMainResources(properties(initialSecurityProps, expectedSecurityProps, s -> s.path("application.properties")))));
    }

    @Test
    void updateYamlConfig() {
        rewriteRun(mavenProject("project", srcMainResources(yaml(initialSecurityYaml, expectedSecurityYaml, s -> s.path("application.yml")))));
    }

    @Test
    void updatePartialYamlConfig() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          mavenProject("project", srcMainResources(yaml(initialSecurityYamlPartial, expectedSecurityYamlPartial, s -> s.path("application.yml")))));
    }

    @Test
    void noJwtConfig() {
        rewriteRun(mavenProject("project", srcMainResources(yaml(noJwtConfig, s -> s.path("application.yml")))));
    }

    @Test
    void notMicronautConfigYaml() {
        rewriteRun(mavenProject("project", srcMainResources(yaml(initialSecurityYaml, s -> s.path("foo.yml")))));
    }

    @Test
    void notMicronautConfigProperties() {
        rewriteRun(mavenProject("project", srcMainResources(properties(initialSecurityProps, s -> s.path("foo.properties")))));
    }
}
