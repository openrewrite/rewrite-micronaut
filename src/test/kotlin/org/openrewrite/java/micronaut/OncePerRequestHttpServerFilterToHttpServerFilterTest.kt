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

class OncePerRequestHttpServerFilterToHttpServerFilterTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = OncePerRequestHttpServerFilterToHttpServerFilter()
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .classpath("micronaut-http", "micronaut-core")
            .dependsOn(
                """
                package a.b;
                public interface C {
                    String getCName();
                }
            """
            ).build()

    @Test
    fun simpleConversionWithExistingImplements() = assertChanged(
        before = """
            package a.b;

            import io.micronaut.core.order.Ordered;
            import io.micronaut.http.HttpRequest;
            import io.micronaut.http.MutableHttpResponse;
            import io.micronaut.http.filter.OncePerRequestHttpServerFilter;
            import io.micronaut.http.filter.ServerFilterChain;
            import org.reactivestreams.Publisher;
            
            public class MyServerFilter extends OncePerRequestHttpServerFilter implements C {
                @Override
                public int getOrder() {
                    return Ordered.LOWEST_PRECEDENCE;
                }
                
                @Override
                public Publisher<MutableHttpResponse<?>> doFilterOnce(HttpRequest<?> request, ServerFilterChain chain) {
                    getKey(MyServerFilter.class);
                }
                
                @Override
                public String getCName() {
                    return "cname";
                }
            }
        """,
        after = """
            package a.b;
            
            import io.micronaut.core.order.Ordered;
            import io.micronaut.http.HttpRequest;
            import io.micronaut.http.MutableHttpResponse;
            import io.micronaut.http.filter.HttpServerFilter;
            import io.micronaut.http.filter.ServerFilterChain;
            import org.reactivestreams.Publisher;
            
            public class MyServerFilter implements C, HttpServerFilter {
                @Override
                public int getOrder() {
                    return Ordered.LOWEST_PRECEDENCE;
                }
                
                @Override
                public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
                    /*TODO: See `Server Filter Behavior` in https://docs.micronaut.io/3.0.x/guide/#breaks for details*/ getKey(MyServerFilter.class);
                }
                
                @Override
                public String getCName() {
                    return "cname";
                }
            }
        """
    )

    @Test
    fun simpleConversion() = assertChanged(
        before = """
            package a.b;

            import io.micronaut.core.order.Ordered;
            import io.micronaut.http.HttpRequest;
            import io.micronaut.http.MutableHttpResponse;
            import io.micronaut.http.filter.OncePerRequestHttpServerFilter;
            import io.micronaut.http.filter.ServerFilterChain;
            import org.reactivestreams.Publisher;
            
            public class MyServerFilter extends OncePerRequestHttpServerFilter {
                @Override
                public int getOrder() {
                    return Ordered.LOWEST_PRECEDENCE;
                }
                
                @Override
                public Publisher<MutableHttpResponse<?>> doFilterOnce(HttpRequest<?> request, ServerFilterChain chain) {
                    getKey(MyServerFilter.class);
                }
                
                @Override
                public String getCName() {
                    return "cname";
                }
            }
        """,
        after = """
            package a.b;
            
            import io.micronaut.core.order.Ordered;
            import io.micronaut.http.HttpRequest;
            import io.micronaut.http.MutableHttpResponse;
            import io.micronaut.http.filter.HttpServerFilter;
            import io.micronaut.http.filter.ServerFilterChain;
            import org.reactivestreams.Publisher;
            
            public class MyServerFilter implements HttpServerFilter {
                @Override
                public int getOrder() {
                    return Ordered.LOWEST_PRECEDENCE;
                }
                
                @Override
                public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
                    /*TODO: See `Server Filter Behavior` in https://docs.micronaut.io/3.0.x/guide/#breaks for details*/ getKey(MyServerFilter.class);
                }
                
                @Override
                public String getCName() {
                    return "cname";
                }
            }
        """
    )
}