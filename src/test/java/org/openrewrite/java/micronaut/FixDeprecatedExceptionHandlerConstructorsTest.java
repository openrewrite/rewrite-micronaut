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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class FixDeprecatedExceptionHandlerConstructorsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "micronaut-core-2.5.13",
            "micronaut-http-server-2.5.13",
            "micronaut-http-server-netty-2.5.13",
            "micronaut-http-2.5.13",
            "micronaut-validation-2.5.13",
            "jakarta.inject-api-2.*",
            "validation-api-2.*"))
          .recipe(new FixDeprecatedExceptionHandlerConstructors());
    }

    @DocumentExample
    @Test
    void addsErrorProcessorConstructor() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
              package abc;
              
              import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
              
              public class ApiClientValidationExceptionHandler extends ConstraintExceptionHandler {
                  private void someMethod(){}
              }
              """,
            """
              package abc;
              
              import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
              import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
              import jakarta.inject.Inject;
              
              public class ApiClientValidationExceptionHandler extends ConstraintExceptionHandler {
              
                  @Inject
                  public ApiClientValidationExceptionHandler(ErrorResponseProcessor errorResponseProcessor) {
                      super(errorResponseProcessor);
                  }
                  private void someMethod(){}
              }
              """)
        );
    }

    @Test
    void addsErrorProcessorConstructorArg() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
              package abc;
              
              import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
              
              public class ApiClientValidationExceptionHandler extends ConstraintExceptionHandler {
              
                  public ApiClientValidationExceptionHandler() {
                  }
              }
              """,
            """
              package abc;
              
              import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
              import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
              import jakarta.inject.Inject;
              
              public class ApiClientValidationExceptionHandler extends ConstraintExceptionHandler {
              
                  @Inject
                  public ApiClientValidationExceptionHandler(ErrorResponseProcessor errorResponseProcessor) {
                      super(errorResponseProcessor);
                  }
              }
              """
          )
        );
    }

    @Test
    void updatesErrorProcessorConstructor() {
        rewriteRun(java(
            """
              package abc;
              
              import io.micronaut.http.server.exceptions.ConversionErrorHandler;
              
              public class ApiClientValidationExceptionHandler extends ConversionErrorHandler {
              
                  public ApiClientValidationExceptionHandler() {
                      super();
                  }
              }
              """,
            """
              package abc;
              
              import io.micronaut.http.server.exceptions.ConversionErrorHandler;
              import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
              import jakarta.inject.Inject;
              
              public class ApiClientValidationExceptionHandler extends ConversionErrorHandler {
              
                  @Inject
                  public ApiClientValidationExceptionHandler(ErrorResponseProcessor errorResponseProcessor) {
                      super(errorResponseProcessor);
                  }
              }
              """
          )
        );
    }

    @Test
    void addsErrorProcessorConstructorParamToExistingParams() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
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
            """
              package abc;
              import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
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
              """

          )
        );
    }
}