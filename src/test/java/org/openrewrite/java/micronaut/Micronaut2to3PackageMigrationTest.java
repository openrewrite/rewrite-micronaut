/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class Micronaut2to3PackageMigrationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.micronaut.Micronaut2to3Migration")
          .parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package io.micronaut.configuration.security.ldap;
              public class LdapAuthenticationProvider {}
              """,
            """
              package io.micronaut.configuration.security.ldap.context;
              public class LdapSearchService {}
              """
          ));
    }

    @DocumentExample
    @Test
    void migrateLdapType() {
        rewriteRun(
          //language=java
          java(
            """
              import io.micronaut.configuration.security.ldap.LdapAuthenticationProvider;

              class A {
                  LdapAuthenticationProvider provider;
              }
              """,
            """
              import io.micronaut.security.ldap.LdapAuthenticationProvider;

              class A {
                  LdapAuthenticationProvider provider;
              }
              """
          )
        );
    }

    @Test
    void migrateLdapTypeInSubpackage() {
        rewriteRun(
          //language=java
          java(
            """
              import io.micronaut.configuration.security.ldap.context.LdapSearchService;

              class A {
                  LdapSearchService service;
              }
              """,
            """
              import io.micronaut.security.ldap.context.LdapSearchService;

              class A {
                  LdapSearchService service;
              }
              """
          )
        );
    }

    @Test
    void migrateJsr305NullabilityAnnotations() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "micronaut-core-2.*")
            .dependsOn(
              """
                package javax.annotation;
                public @interface Nullable {}
                """,
              """
                package javax.annotation;
                public @interface Nonnull {}
                """
            )),
          //language=java
          java(
            """
              import javax.annotation.Nonnull;
              import javax.annotation.Nullable;

              class A {
                  @Nullable
                  String name;

                  @Nonnull
                  String id() {
                      return "";
                  }
              }
              """,
            """
              import io.micronaut.core.annotation.NonNull;
              import io.micronaut.core.annotation.Nullable;

              class A {
                  @Nullable
                  String name;

                  @NonNull
                  String id() {
                      return "";
                  }
              }
              """
          )
        );
    }
}
