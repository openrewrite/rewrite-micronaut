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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class BeanPropertyCapitalizationStrategyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "micronaut-core-2.5.13").dependsOn("""
              package a.b;
              public class C {
                  public String getCName() {
                      return "";
                  }
              }
          """)).recipe(new BeanPropertyCapitalizationStrategy());
    }

    @Test
    void deCapitalizeProperty() {
        rewriteRun(java(
          """
            package a.b;
            import io.micronaut.core.beans.BeanIntrospection;
            import io.micronaut.core.beans.BeanProperty;
            import org.checkerframework.checker.units.qual.C;
                
            class T {
                void p() {
                    BeanIntrospection<C> introspection = BeanIntrospection.getIntrospection(C.class);
                    BeanProperty<C, String> p1 = introspection.getRequiredProperty("CName", String.class);
                    Optional<BeanProperty<C, String>> p2 = introspection.getProperty("CName", String.class);
                    Optional<BeanProperty<C, Object>> p3 = introspection.getProperty("CName");
                }
            }
            """,
          """
            package a.b;
            import io.micronaut.core.beans.BeanIntrospection;
            import io.micronaut.core.beans.BeanProperty;
            import org.checkerframework.checker.units.qual.C;
            
            class T {
                void p() {
                    BeanIntrospection<C> introspection = BeanIntrospection.getIntrospection(C.class);
                    BeanProperty<C, String> p1 = introspection.getRequiredProperty("cName", String.class);
                    Optional<BeanProperty<C, String>> p2 = introspection.getProperty("cName", String.class);
                    Optional<BeanProperty<C, Object>> p3 = introspection.getProperty("cName");
                }
            }
            """));
    }
}
