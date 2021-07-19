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
}