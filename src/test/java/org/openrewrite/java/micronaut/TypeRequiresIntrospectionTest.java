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

import static org.openrewrite.java.Assertions.java;

class TypeRequiresIntrospectionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "micronaut-core-2.5.13", "micronaut-http-2.5.13", "micronaut-http-client-core-2.5.13"))
          .recipe(new TypeRequiresIntrospection());
    }

    @Language("java")
    String pojoD =
      """
            package a.b;
            import io.micronaut.core.annotation.Introspected;
            @Introspected
            public class D {
                String name;
                String getName() { return name;}
                void setName(String name) {this.name = name;}
            }
        """;
    @Language("java")
    String controllerClass =
      """
            package a.b;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            
            @Controller
            public class AbController {
                @Get
                public String getAbName(C c) {
                    return c.getName();
                }
                @Get
                public String getDName(D d) {
                    return d.getName();
                }
                private void doNothing() {}
                private int getSomething() {return 0;}
            }
        """;

    @Test
    void hasIntrospectionAnnotation() {
        rewriteRun(
          java(controllerClass),
          java(pojoD),
          java(
            """
              package a.b;
              
              import io.micronaut.core.annotation.Introspected;
              
              @Introspected
              public class C {
                  String name;
                  String getName() { return name;}
                  void setName(String name) {this.name = name;}
              }
              """
          )
        );
    }

    @Test
    void addsIntrospectionAnnotationFromParameter() {
        rewriteRun(
          java(controllerClass),
          java(pojoD),
          java(
            """
              package a.b;
              
              public class C {
                  String name;
                  String getName() { return name;}
                  void setName(String name) {this.name = name;}
              }
              """,
            """
              package a.b;
              
              import io.micronaut.core.annotation.Introspected;
              
              @Introspected
              public class C {
                  String name;
                  String getName() { return name;}
                  void setName(String name) {this.name = name;}
              }
              """
          )
        );
    }

    @Test
    void addsIntrospectionAnnotationForOptional() {
        rewriteRun(
          java(
            """
              package a.b;
              import io.micronaut.http.annotation.Get;
              import io.micronaut.http.client.annotation.Client;
              import java.util.Optional;
              
              @Client
              public interface AbcClient {
                  @Get
                  Optional<C> getC(long cId);
              }
              """
          ),
          java(
            """
              package a.b;
              
              public class C {
              }
              """,
            """
              package a.b;
              
              import io.micronaut.core.annotation.Introspected;
              
              @Introspected
              public class C {
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeServiceOrController() {
        rewriteRun(
          java(
            """
              package a.b;
              import io.micronaut.http.annotation.Controller;
              
              @Controller
              public class AbController {
                  AbcClient abcClient;
                  
                  public AbcClient client() {
                      return abcClient;
                  }
              }
              """
          ),
          java(
            """
              package a.b;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              import io.micronaut.http.client.annotation.Client;
              import java.util.Optional;
              
              @Client
              public interface AbcClient {
                  @Get
                  Optional<String> getC(long cId);
              }
              """
          )
        );
    }

    @Test
    void addsIntrospectionAnnotationFromReturnType() {
        rewriteRun(
          java(pojoD),
          java(
            """
              package a.b;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Get;
              
              @Controller
              public class AbController {
                  @Get
                  public C getC() {
                      return new C();
                  }
                  private void doNothing() {}
                  private int getSomething() {return 0;}
              }
              """
          ),
          java(
            """
              package a.b;
              
              public class C {
                  String name;
                  String getName() { return name;}
                  void setName(String name) {this.name = name;}
              }
              """,
            """
              package a.b;
              
              import io.micronaut.core.annotation.Introspected;
              
              @Introspected
              public class C {
                  String name;
                  String getName() { return name;}
                  void setName(String name) {this.name = name;}
              }
              """
          )
        );
    }

    @Test
    void addsIntrospectionAnnotationForParameterizedParam() {
        rewriteRun(
          java(
            pojoD),
          java(
            """
              package a.b;
              import io.micronaut.http.annotation.Controller;
              import io.micronaut.http.annotation.Post;import java.util.List;
              
              @Controller
              public class AbController {
                  @Post
                  public void getC(List<C> cList) {
                  }
              }
              """
          ),
          java(
            """
              package a.b;
              
              public class C {
              }
              """,
            """
              package a.b;
              
              import io.micronaut.core.annotation.Introspected;
              
              @Introspected
              public class C {
              }
              """
          )
        );
    }

    @Test
    void serviceShouldNotBeIntrospected() {
        rewriteRun(
          java(
            """
              package a.b;
              import io.micronaut.http.annotation.Controller;
              
              @Controller
              public class AbController {
                  private final CdService cdService;
                  public AbController(CdService cdService) {
                      this.cdService = cdService;
                  }
              }
              """
          ),
          java(
            """
              package a.b;

              import io.micronaut.http.client.annotation.Client;
              
              @Client
              public class CdService {
              }
              """
          )
        );
    }

    @Test
    void serviceShouldNotBeIntrospectedBecauseOfTest() {
        rewriteRun(
          java(
            """
              package api.model;

              import io.micronaut.core.annotation.Introspected;
              
              @Introspected
              public class Event {
              }
              """
          ),
          java(
            """
              package api.services.support;

              import api.model.Event;
              import api.services.EventsService;
              import io.micronaut.http.annotation.Controller;
              
              @Controller
              public class TrackEventFilter {
                  private final EventsService eventsService;
              
                  public TrackEventFilter(EventsService eventsService) {
                      this.eventsService = eventsService;
                  }
              
                  private Event trackEventError(String event, Throwable throwable) {
                      return eventsService.trackEvents("api", "id", new Event());
                  }
              }
              """
          ),
          java(
            """
                  package api.services;

                  import api.model.Event;
                  import io.micronaut.http.annotation.Controller;
                  import io.micronaut.http.annotation.Post;
                  import io.micronaut.http.client.annotation.Client;
                  
                  @Controller("/api")
                  @Client("eventService")
                  public interface EventsService {
                      @Post("/events")
                      Boolean trackEvents(String source, String track, Event... events);
                  }

              """
          )
        );
    }
}