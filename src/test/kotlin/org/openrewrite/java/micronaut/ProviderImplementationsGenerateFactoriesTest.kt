package org.openrewrite.java.micronaut

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class ProviderImplementationsGenerateFactoriesTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .classpath("micronaut-core", "javax.inject")
            .dependsOn("""
                package abc;
                public class A {}
            """)
            .build()
    override val recipe: Recipe
        get() = ProviderImplementationsToMicronautFactories()

    @Test
    fun addsFactoryAnnotation() = assertChanged(
        before = """
            package abc;
            import javax.inject.Provider;
            import javax.inject.Singleton;
            
            @Singleton
            public class AProvider implements Provider<A> {
            
                @Override
                public A get() {
                    return new AImpl();
                }
                
                private void doSomething() {
                }
            }
        """,
        after = """
            package abc;
            import io.micronaut.context.annotation.Factory;
            
            import javax.inject.Provider;
            import javax.inject.Singleton;
            
            @Factory
            public class AProvider implements Provider<A> {
            
                @Override
                @Singleton
                public A get() {
                    return new AImpl();
                }
                
                private void doSomething() {
                }
            }
        """
    )

    @Test
    fun notSingletonNoChange() = assertUnchanged(
        before = """
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

    @Test
    fun isFactoryNoChange() = assertUnchanged(
        before = """
            package abc;
            import io.micronaut.context.annotation.Factory;
            
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
}