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

class FactoryBeansAreTypedTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = FactoryBeansAreTyped()
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .classpath("micronaut-core", "micronaut-inject", "javax.inject", "jakarta.inject-api")
            .build()

    @Test
    fun beanTypeMatchesReturnType() = assertUnchanged(
        before = """
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

    @Test
    fun notAFactory() = assertUnchanged(
        before = """
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

    @Test
    fun addsTypeForInternalImplementation() = assertChanged(
        before = """
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
        after = """
            import java.util.concurrent.ForkJoinPool;
            import java.util.concurrent.ExecutorService;
            import javax.inject.Singleton;
            
            import io.micronaut.context.annotation.Bean;
            import io.micronaut.context.annotation.Factory;
            
            @Factory
            public class ExecutorFactory {
            
                @Bean(typed = {ExecutorService.class, ForkJoinPool.class})
                @Singleton
                public ExecutorService executorService() {
                    return ForkJoinPool.commonPool();
                }
            }
        """
    )

    @Test
    fun addsTypeForInternalImplementationJakarta() = assertChanged(
        before = """
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
        after = """
            import java.util.concurrent.ForkJoinPool;
            import java.util.concurrent.ExecutorService;
            import jakarta.inject.Singleton;
            import io.micronaut.context.annotation.Bean;
            import io.micronaut.context.annotation.Factory;
            
            @Factory
            public class ExecutorFactory {
            
                @Bean(typed = {ExecutorService.class, ForkJoinPool.class})
                @Singleton
                public ExecutorService executorService() {
                    return ForkJoinPool.commonPool();
                }
            }
        """
    )

    @Test
    fun returnTypeIsNewClass() = assertChanged(
        dependsOn = arrayOf(
            """
                package abc;
                public interface MyInterface {}
            """,
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
            """,
            """
                package abc;
                public class MyGenericThing<T> {}
            """,
            """
                package abc;
                public class MyOtherGenericThing<T> extends MyGenericThing<T> {}
            """
        ),
        before = """
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
        after = """
            package abc;
            import jakarta.inject.Singleton;
            import io.micronaut.context.annotation.Bean;
            import io.micronaut.context.annotation.Factory;
            
            @Factory
            public class ExecutorFactory {
            
                @Bean(typed = {MyInterface.class, MyThing.class})
                @Singleton
                public MyInterface myInterface() {
                    try {
                        return new MyThing("some name");
                    } catch (Exception ex) {
                        return null;
                    }
                }
            }
        """
    )
}