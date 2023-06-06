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

import static org.openrewrite.java.Assertions.java;

class ProviderImplementationsGenerateFactoriesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
              "micronaut-core-2.5.13", "micronaut-inject-2.5.13", "javax.inject-1", "jakarta.inject-api-2.*")
            .dependsOn(
              //language=java
              """
                package abc;
                public class A {}
                public class AImpl extends A {}
                """
            )
          )
          .recipe(new ProviderImplementationsToMicronautFactories());
    }

    @Test
    void javaxProviderImplementation() {
        rewriteRun(
          java(
            """
              package abc;

              import io.micronaut.context.annotation.Bean;
              import io.micronaut.core.annotation.NonNull;
              import javax.inject.Provider;
              import javax.inject.Singleton;

              @Bean
              @Singleton
              public class AProvider implements Provider<A> {

                  @Override
                  public A get() {
                      return new AImpl();
                  }

                  private void doSomething(@NonNull String arg) {
                  }
              }
              """,
            """
              package abc;

              import io.micronaut.context.annotation.Bean;
              import io.micronaut.context.annotation.Factory;
              import io.micronaut.core.annotation.NonNull;
              import javax.inject.Provider;
              import javax.inject.Singleton;

              @Factory
              public class AProvider implements Provider<A> {

                  @Override
                  @Bean
                  @Singleton
                  public A get() {
                      return new AImpl();
                  }

                  private void doSomething(@NonNull String arg) {
                  }
              }
              """
          )
        );
    }

    @Test
    void jakartaProviderImplementation() {
        rewriteRun(
          java(
            """
              package abc;
              import io.micronaut.context.annotation.Prototype;
              import io.micronaut.core.annotation.NonNull;
              import jakarta.inject.Provider;

              @Prototype
              public class AProvider implements Provider<A> {

                  @Override
                  public A get() {
                      return new AImpl();
                  }

                  @NonNull
                  private String doSomething() {
                  }
              }
              """,
            """
              package abc;
              import io.micronaut.context.annotation.Factory;
              import io.micronaut.context.annotation.Prototype;
              import io.micronaut.core.annotation.NonNull;
              import jakarta.inject.Provider;

              @Factory
              public class AProvider implements Provider<A> {

                  @Override
                  @Prototype
                  public A get() {
                      return new AImpl();
                  }

                  @NonNull
                  private String doSomething() {
                  }
              }
              """
          )
        );
    }

    @Test
    void notBeanNoChange() {
        rewriteRun(
          java(
            """
              package abc;
              import javax.inject.Provider;
                            
              public class AProvider implements Provider<A> {
                            
                  @Override
                  public A get() {
                      return new AImpl();
                  }
              }
              """
          )
        );
    }

    @Test
    void isFactoryNoChange() {
        rewriteRun(
          java(
            """
              package abc;
              import io.micronaut.context.annotation.Factory;
              import org.checkerframework.checker.units.qual.A;

              import javax.inject.Provider;
              import javax.inject.Singleton;
                            
              @Factory
              public class AProvider implements Provider<A> {

                  @Singleton
                  @Override
                  public A get() {
                      return new AImpl();
                  }
              }
              """
          )
        );
    }
}