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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class FixDeprecatedExceptionHandlerConstructorsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath(
            "micronaut-core",
            "micronaut-http-server-netty",
            "micronaut-http",
            "micronaut-validation",
            "jakarta.inject-api",
            "validation-api"))
          .recipe(new FixDeprecatedExceptionHandlerConstructors());
    }

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