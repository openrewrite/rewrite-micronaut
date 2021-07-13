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
package org.openrewrite.java.micronaut

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class BeanPropertyCapitalizationStrategyTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .classpath("micronaut-core")
            .dependsOn("""
                package a.b;
                public class C {
                    public String getCName() {
                        return "";
                    }
                }
            """)
            .build()
    override val recipe: Recipe
        get() = BeanPropertyCapitalizationStrategy()

    @Test
    fun deCapitalizeProperty() = assertChanged(
        before = """
            package a.b;
            import io.micronaut.core.beans.BeanIntrospection;
            import io.micronaut.core.beans.BeanProperty;
            
            class T {
                void p() {
                    BeanIntrospection<C> introspection = BeanIntrospection.getIntrospection(C.class);
                    BeanProperty<C, String> p1 = introspection.getRequiredProperty("CName", String.class);
                    Optional<BeanProperty<C, String>> p2 = introspection.getProperty("CName", String.class);
                    Optional<BeanProperty<C, Object>> p3 = introspection.getProperty("CName");
                }
            }
        """,
        after = """
            package a.b;
            import io.micronaut.core.beans.BeanIntrospection;
            import io.micronaut.core.beans.BeanProperty;
            
            class T {
                void p() {
                    BeanIntrospection<C> introspection = BeanIntrospection.getIntrospection(C.class);
                    BeanProperty<C, String> p1 = introspection.getRequiredProperty("cName", String.class);
                    Optional<BeanProperty<C, String>> p2 = introspection.getProperty("cName", String.class);
                    Optional<BeanProperty<C, Object>> p3 = introspection.getProperty("cName");
                }
            }
        """
    )
}
