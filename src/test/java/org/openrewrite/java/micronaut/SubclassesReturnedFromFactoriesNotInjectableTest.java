/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SubclassesReturnedFromFactoriesNotInjectableTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath(
            "micronaut-core", "micronaut-inject", "javax.inject", "jakarta.inject-api"))
          .recipe(new SubclassesReturnedFromFactoriesNotInjectable());
    }

    @Test
    void beanTypeMatchesReturnType() {
        rewriteRun(
          java(
            """
                  import java.util.concurrent.ForkJoinPool;
                  import javax.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  @Factory
                  public class ExecutorFactory {
                  
                      @Singleton
                      public ForkJoinPool executorService() {
                          return ForkJoinPool.commonPool();
                      }
                  }
              """
          )
        );
    }

    @Test
    void notAFactory() {
        rewriteRun(
          java(
            """
                  import java.util.concurrent.ExecutorService;
                  import java.util.concurrent.ForkJoinPool;
                  import javax.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  public class ExecutorFactory {
                      
                      @Singleton
                      public ExecutorService executorService() {
                          return ForkJoinPool.commonPool();
                      }
                  }
              """
          )
        );
    }

    @Test
    void addsTypeForInternalImplementation() {
        rewriteRun(
          java(
            """
                  import java.util.concurrent.ForkJoinPool;
                  import java.util.concurrent.ExecutorService;
                  import javax.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  @Factory
                  public class ExecutorFactory {
                      
                      @Singleton
                      public ExecutorService executorService() {
                          return ForkJoinPool.commonPool();
                      }
                  }
              """,
            """
                  import java.util.concurrent.ForkJoinPool;
                  import javax.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  @Factory
                  public class ExecutorFactory {
                  
                      @Singleton
                      public ForkJoinPool executorService() {
                          return ForkJoinPool.commonPool();
                      }
                  }
              """
          )
        );
    }

    @Test
    void addsTypeForInternalImplementationJakarta() {
        rewriteRun(
          java(
            """
                  import java.util.concurrent.ForkJoinPool;
                  import java.util.concurrent.ExecutorService;
                  import jakarta.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  @Factory
                  public class ExecutorFactory {
                  
                      @Singleton
                      public ExecutorService executorService() {
                          return ForkJoinPool.commonPool();
                      }
                  }
              """,
            """
                  import java.util.concurrent.ForkJoinPool;
                  import jakarta.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  @Factory
                  public class ExecutorFactory {
                  
                      @Singleton
                      public ForkJoinPool executorService() {
                          return ForkJoinPool.commonPool();
                      }
                  }
              """
          )
        );
    }


    @Test
    void returnTypeIsNewClass() {
        rewriteRun(
          java(
            """
                  package abc;
                  public interface MyInterface {}
              """
          ),
          java(
            """
                  package abc;
                  public class MyThing implements MyInterface {
                      private String name;
                      public MyThing(String name) {
                          if (name == null) {
                              throw new IllegalArgumentException();
                          }
                          this.name = name;
                      }
                  }
              """
          ),
          java(
            """
                  package abc;
                  import jakarta.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  @Factory
                  public class ExecutorFactory {
                  
                      @Singleton
                      public MyInterface myInterface() {
                          try {
                              return new MyThing("some name");
                          } catch (Exception ex) {
                              return null;
                          }
                      }
                  }
              """,
            """
                  package abc;
                  import jakarta.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  @Factory
                  public class ExecutorFactory {
                  
                      @Singleton
                      public MyThing myInterface() {
                          try {
                              return new MyThing("some name");
                          } catch (Exception ex) {
                              return null;
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void multipleReturnTypes() {
        rewriteRun(
          java(
            """
                  package abc;
                  public class MyConfig {
                      public boolean isSomething() {
                          return false;
                      }
                  }
              """),
          java(
            """
                  package abc;
                  public interface MyInterface {}
              """),
          java(
            """
                  package abc;
                  public class MyThing implements MyInterface {
                      private String name;
                      public MyThing(String name) {
                          if (name == null) {
                              throw new IllegalArgumentException();
                          }
                          this.name = name;
                      }
                  }
              """),
          java(
            """
                  package abc;
                  public class MyOtherThing implements MyInterface {
                      private String name;
                      public MyOtherThing(String name) {
                          if (name == null) {
                              throw new IllegalArgumentException();
                          }
                          this.name = name;
                      }
                  }
              """),
          java(
            """
                  package abc;
                  import jakarta.inject.Inject;
                  import jakarta.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  @Factory
                  public class ExecutorFactory {
                      @Inject
                      MyConfig myConfig;
                      
                      @Singleton
                      public MyInterface myInterface() {
                          try {
                              if (myConfig.isSomething()) {
                                  return new MyThing("some name");
                              } else {
                                  return new MyOtherThing("some other thing");
                              }
                          } catch (Exception ex) {
                              return null;
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void parameterizedReturnTypes() {
        rewriteRun(
          java(
            """
                  package abc;
                  public interface Qi<T> {}
              """),
          java(
            """
                  package abc;
                  public class Kq<T> implements Qi<T> {
                      T t;
                      public Kq(T t) {
                          this.t = t;
                      }
                  }
              """
          ),
          java(
            """
                  package abc;
                  import jakarta.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  @Factory
                  public class ExecutorFactory {
                  
                      @Singleton
                      public Qi<String> t() {
                          return new Kq<>("b");
                      }
                      
                      @Singleton
                      public Qi t() {
                          return new Kq<String>("b");
                      }
                  }
              """,
            """
                  package abc;
                  import jakarta.inject.Singleton;
                  import io.micronaut.context.annotation.Factory;
                  
                  @Factory
                  public class ExecutorFactory {
                  
                      @Singleton
                      public Kq<String> t() {
                          return new Kq<>("b");
                      }
                  
                      @Singleton
                      public Kq t() {
                          return new Kq<String>("b");
                      }
                  }
              """
          )
        );
    }
}
