package org.openrewrite.java.micronaut

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class ExceptionHandlersHaveErrorResponseProcessorConstructorArgumentTest : JavaRecipeTest {

    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("micronaut-core", "micronaut-http-server-netty", "micronaut-http", "micronaut-validation", "jakarta.inject-api", "validation-api").build()
    override val recipe: Recipe
        get() = ExceptionHandlersHaveErrorResponseProcessorConstructorArgument()

    @Test
    fun addsErrorProcessorConstructor() = assertChanged(
        before = """
            package abc;
            import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
            public class ApiClientValidationExceptionHandler extends ConstraintExceptionHandler {
                public ApiClientValidationExceptionHandler() {
                }
            }
        """,
        after = """
            package abc;
            import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
            import jakarta.inject.Inject;
            
            public class ApiClientValidationExceptionHandler extends ConstraintExceptionHandler {
            
                @Inject
                public ApiClientValidationExceptionHandler(ErrorResponseProcessor errorResponseProcessor) {
                    super(errorResponseProcessor);
                }
            }
        """,
        typeValidation = {methodInvocations = false}
    )

    @Test
    fun updatesErrorProcessorConstructor() = assertChanged(
        before = """
            package abc;
            
            import io.micronaut.http.server.exceptions.ConversionErrorHandler;
            public class ApiClientValidationExceptionHandler extends ConversionErrorHandler {
                public ApiClientValidationExceptionHandler() {
                    super();
                }
            }
        """,
        after = """
            package abc;
            
            import io.micronaut.http.server.exceptions.ConversionErrorHandler;
            import jakarta.inject.Inject;
            
            public class ApiClientValidationExceptionHandler extends ConversionErrorHandler {
            
                @Inject
                public ApiClientValidationExceptionHandler(ErrorResponseProcessor errorResponseProcessor) {
                    super(errorResponseProcessor);
                }
            }
        """
    )

    @Test
    fun addsErrorProcessorConstructorParamToExistingParams() = assertChanged(
        before = """
            package abc;
            import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
            import jakarta.inject.Inject;
            
            public class ApiClientValidationExceptionHandler extends ConstraintExceptionHandler {
                private final String conversionService;
                
                @Inject
                public ApiClientValidationExceptionHandler(String conversionService) {
                    this.conversionService = conversionService;
                }
            }
        """,
        after = """
            package abc;
            import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
            import jakarta.inject.Inject;
            
            public class ApiClientValidationExceptionHandler extends ConstraintExceptionHandler {
                private final String conversionService;
            
                @Inject
                public ApiClientValidationExceptionHandler(String conversionService, ErrorResponseProcessor errorResponseProcessor) {
                    super(errorResponseProcessor);
                    this.conversionService = conversionService;
                }
            }
        """,
        typeValidation = {methodInvocations = false}
    )
}