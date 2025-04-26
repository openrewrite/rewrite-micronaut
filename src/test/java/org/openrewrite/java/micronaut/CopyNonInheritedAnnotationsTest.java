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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CopyNonInheritedAnnotationsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "micronaut-core-2.5.13", "micronaut-context-2.5.13", "micronaut-http-2.5.13", "micronaut-http-client-core-2.5.13")
          //language=java
          .dependsOn("""
                package a.b;
                public class C {
                    public String getCName() {
                        return "";
                    }
                }
            """)).recipe(new CopyNonInheritedAnnotations());
    }

    @DocumentExample
    @Test
    void refreshableFromGrandparent() {
        //language=java
        rewriteRun(
          java(
            """
              package abc;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Refreshable
              public abstract class BaseController {
              }
              
              public abstract class MiddleController extends BaseController {
              }
              """,
            """
              package abc;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Refreshable
              public abstract class BaseController {
              }
              
              @Refreshable
              public abstract class MiddleController extends BaseController {
              }
              """
          ),
          java(
            """
              package abc;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              
              public class SuperClass {
              
                  @Controller
                  public class MyController extends MiddleController {
                      @Get
                      public String info() {
                          return "system info: ";
                      }
                  }
              }
              """,
            """
              package abc;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              public class SuperClass {
              
                  @Controller
                  @Refreshable
                  public class MyController extends MiddleController {
                      @Get
                      public String info() {
                          return "system info: ";
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void refreshableControllerExtends() {
        //language=java
        rewriteRun(
          java(
            """
              package abc;
              import io.micronaut.core.version.annotation.Version;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Refreshable
              @Version("0.1")
              public interface BaseController {
              }
              """
          ),
          java(
            """
              package abc;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              
              @Controller
              public class MyController implements BaseController {
                  @Get
                  public String info() {
                      return "system info: ";
                  }
              }
              """,
            """
              package abc;
              import io.micronaut.core.version.annotation.Version;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Controller
              @Refreshable
              @Version("0.1")
              public class MyController implements BaseController {
                  @Get
                  public String info() {
                      return "system info: ";
                  }
              }
              """
          )
        );
    }

    @Test
    void refreshableParameterized() {
        //language=java
        rewriteRun(
          java(
            """
              package abc;
              public interface Thing<T> {
                  T getThing();
              }
              """
          ),
          java(
            """
              package abc;
              
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Refreshable
              public class BaseThing implements Thing<String> {
                  @Override
                  String getThing() {
                      return "thing";
                  }
              }
              """
          ),
          java(
            """
              package abc;
              
              public class MyController extends BaseThing {
              }
              """,
            """
              package abc;
              
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Refreshable
              public class MyController extends BaseThing {
              }
              """
          )
        );
    }

    @Test
    void refreshableController() {
        //language=java
        rewriteRun(
          java(
            """
              package abc;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Refreshable
              public abstract class BaseController {
              }
              """
          ),
          java(
            """
              package abc;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              
              @Controller
              public class MyController extends BaseController {
                  @Get
                  public String info() {
                      return "system info: ";
                  }
              }
              """,
            """
              package abc;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Controller
              @Refreshable
              public class MyController extends BaseController {
                  @Get
                  public String info() {
                      return "system info: ";
                  }
              }
              """
          )
        );
    }

    @Test
    void refreshableNestedClass() {
        //language=java
        rewriteRun(
          java(
            """
              package abc;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Refreshable
              public abstract class BaseController {
              }
              """
          ),
          java(
            """
              package abc;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              
              public class SuperClass {
              
                  @Controller
                  public class MyController extends BaseController {
                      @Get
                      public String info() {
                          return "system info: ";
                      }
                  }
              }
              """,
            """
              package abc;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              public class SuperClass {
              
                  @Controller
                  @Refreshable
                  public class MyController extends BaseController {
                      @Get
                      public String info() {
                          return "system info: ";
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void noDuplicateAnnotations() {
        //language=java
        rewriteRun(
          java(
            """
              package abc;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Refreshable
              public abstract class BaseController {
              }
              
              @Refreshable
              public interface BaseControllerInterface {
              }
              """
          ),
          java(
            """
              package abc;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              
              @Controller
              public class MyController extends BaseController implements BaseControllerInterface {
                  @Get
                  public String info() {
                      return "system info: ";
                  }
              }
              """,
            """
              package abc;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              import io.micronaut.runtime.context.scope.Refreshable;

              @Controller
              @Refreshable
              public class MyController extends BaseController implements BaseControllerInterface {
                  @Get
                  public String info() {
                      return "system info: ";
                  }
              }
              """
          )
        );
    }

    @Test
    void combineAnnotationsFromSuperAndInterface() {
        //language=java
        rewriteRun(
          java(
            """
              package abc;
              import io.micronaut.core.version.annotation.Version;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Refreshable
              public abstract class BaseController {
              }
              
              @Refreshable
              @Version("0.1")
              public interface BaseControllerInterface {
              }
              """
          ),
          java(
            """
              package abc;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              
              @Controller
              public class MyController extends BaseController implements BaseControllerInterface {
                  @Get
                  public String info() {
                      return "system info: ";
                  }
              }
              """,
            """
              package abc;
              import io.micronaut.core.version.annotation.Version;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              import io.micronaut.runtime.context.scope.Refreshable;

              @Controller
              @Refreshable
              @Version("0.1")
              public class MyController extends BaseController implements BaseControllerInterface {
                  @Get
                  public String info() {
                      return "system info: ";
                  }
              }
              """
          )
        );
    }

    @Test
    void doNothingIfAnnotationsAlreadyInPlace() {
        //language=java
        rewriteRun(
          java(
            """
              package abc;
              import io.micronaut.core.version.annotation.Version;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Refreshable
              @Version("0.1")
              public abstract class BaseController {
              }
              """
          ),
          java(
            """
              package abc;
              import io.micronaut.core.version.annotation.Version;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              import io.micronaut.runtime.context.scope.Refreshable;
              
              @Controller
              @Refreshable
              @Version("0.1")
              public class MyController extends BaseController {
                  @Get
                  public String info() {
                      return "system info: ";
                  }
              }
              """
          )
        );
    }

    @Disabled("JavaType.Class does not contain method info.  https://github.com/openrewrite/rewrite/issues/150")
    @Test
    void refreshableMethodOverride() {
        //language=java
        rewriteRun(
          java(
            """
              package abc;
              import io.micronaut.core.version.annotation.Version;
              
              public class BaseController {
                  @Version("1.0.0")
                  public void doSomething() {
                  }
              }
              """
          ),
          java(
            """
              package abc;

              public class MyController extends BaseController {
                  @Override
                  public void doSomething() {
                  }
              }
              """,
            """
              package abc;
              import io.micronaut.core.version.annotation.Version;
              
              public class MyController extends BaseController {
                  @Override
                  @Version("1.0.0")
                  public void doSomething() {
                  }
              }
              """
          )
        );
    }
}
