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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class CopyNonInheritedAnnotationsTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .classpath("micronaut-core", "micronaut-context", "micronaut-http", "micronaut-http-client-core").build()
    override val recipe: Recipe
        get() = CopyNonInheritedAnnotations()

    @Test
    fun refreshableControllerExtends() = assertChanged(
        dependsOn = arrayOf(
            """
                package abc;
                import io.micronaut.core.version.annotation.Version;
                import io.micronaut.runtime.context.scope.Refreshable;
                
                @Refreshable
                @Version("0.1")
                public interface BaseController {
                }
            """
        ),
        before = """
            package abc;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            
            @Controller
            public class MyController implements BaseController {
                @Get
                public String info() {
                    return "system info: ";
                }
            }
        """,
        after = """
            package abc;
            import io.micronaut.core.version.annotation.Version;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            import io.micronaut.runtime.context.scope.Refreshable;
            
            @Controller
            @Refreshable
            @Version("0.1")
            public class MyController implements BaseController {
                @Get
                public String info() {
                    return "system info: ";
                }
            }
        """
    )

    @Test
    fun refreshableParameterized() = assertChanged(
        dependsOn = arrayOf(
            """
                package abc;
                public interface Thing<T> {
                    T getThing();
                }
            """,
            """
                package abc;
                
                import io.micronaut.runtime.context.scope.Refreshable;
                
                @Refreshable
                public class BaseThing implements Thing<String> {
                    @Override
                    String getThing() {
                        return "thing";
                    }
                }
            """
        ),
        before = """
            package abc;
            
            public class MyController extends BaseThing {
            }
        """,
        after = """
            package abc;
            
            import io.micronaut.runtime.context.scope.Refreshable;
            
            @Refreshable
            public class MyController extends BaseThing {
            }
        """
    )

    @Test
    fun refreshableController() = assertChanged(
        dependsOn = arrayOf(
            """
                package abc;
                import io.micronaut.runtime.context.scope.Refreshable;
                
                @Refreshable
                public abstract class BaseController {
                }
            """
        ),
        before = """
            package abc;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            
            @Controller
            public class MyController extends BaseController {
                @Get
                public String info() {
                    return "system info: ";
                }
            }
        """,
        after = """
            package abc;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            import io.micronaut.runtime.context.scope.Refreshable;
            
            @Controller
            @Refreshable
            public class MyController extends BaseController {
                @Get
                public String info() {
                    return "system info: ";
                }
            }
        """
    )

    @Test
    fun refreshableNestedClass() = assertChanged(
        dependsOn = arrayOf(
            """
                package abc;
                import io.micronaut.runtime.context.scope.Refreshable;
                
                @Refreshable
                public abstract class BaseController {
                }
            """
        ),
        before = """
            package abc;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            
            public class SuperClass {
            
                @Controller
                public class MyController extends BaseController {
                    @Get
                    public String info() {
                        return "system info: ";
                    }
                }
            }
        """,
        after = """
            package abc;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            import io.micronaut.runtime.context.scope.Refreshable;
            
            public class SuperClass {
            
                @Controller
                @Refreshable
                public class MyController extends BaseController {
                    @Get
                    public String info() {
                        return "system info: ";
                    }
                }
            }
        """
    )

    @Test
    fun refreshableFromGrandparent() = assertChanged(
        dependsOn = arrayOf(
            """
                package abc;
                import io.micronaut.runtime.context.scope.Refreshable;
                
                @Refreshable
                public abstract class BaseController {
                }
                
                public abstract class MiddleController extends BaseController {
                }
            """
        ),
        before = """
            package abc;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            
            public class SuperClass {
            
                @Controller
                public class MyController extends MiddleController {
                    @Get
                    public String info() {
                        return "system info: ";
                    }
                }
            }
        """,
        after = """
            package abc;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            import io.micronaut.runtime.context.scope.Refreshable;
            
            public class SuperClass {
            
                @Controller
                @Refreshable
                public class MyController extends MiddleController {
                    @Get
                    public String info() {
                        return "system info: ";
                    }
                }
            }
        """
    )

    @Test
    fun noDuplicateAnnotations() = assertChanged(
        dependsOn = arrayOf(
            """
                package abc;
                import io.micronaut.runtime.context.scope.Refreshable;
                
                @Refreshable
                public abstract class BaseController {
                }
                
                @Refreshable
                public interface BaseControllerInterface {
                }
            """
        ),
        before = """
            package abc;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            
            @Controller
            public class MyController extends BaseController implements BaseControllerInterface {
                @Get
                public String info() {
                    return "system info: ";
                }
            }
        """,
        after = """
            package abc;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            import io.micronaut.runtime.context.scope.Refreshable;

            @Controller
            @Refreshable
            public class MyController extends BaseController implements BaseControllerInterface {
                @Get
                public String info() {
                    return "system info: ";
                }
            }
        """
    )

    @Test
    fun combineAnnotationsFromSuperAndInterface() = assertChanged(
        dependsOn = arrayOf(
            """
                package abc;
                import io.micronaut.core.version.annotation.Version;
                import io.micronaut.runtime.context.scope.Refreshable;
                
                @Refreshable
                public abstract class BaseController {
                }
                
                @Refreshable
                @Version("0.1")
                public interface BaseControllerInterface {
                }
            """
        ),
        before = """
            package abc;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            
            @Controller
            public class MyController extends BaseController implements BaseControllerInterface {
                @Get
                public String info() {
                    return "system info: ";
                }
            }
        """,
        after = """
            package abc;
            import io.micronaut.core.version.annotation.Version;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            import io.micronaut.runtime.context.scope.Refreshable;

            @Controller
            @Refreshable
            @Version("0.1")
            public class MyController extends BaseController implements BaseControllerInterface {
                @Get
                public String info() {
                    return "system info: ";
                }
            }
        """
    )

    @Test
    fun doNothingIfAnnotationsAlreadyInPlace() = assertUnchanged(
        dependsOn = arrayOf(
            """
                package abc;
                import io.micronaut.core.version.annotation.Version;
                import io.micronaut.runtime.context.scope.Refreshable;
                
                @Refreshable
                @Version("0.1")
                public abstract class BaseController {
                }
            """
        ),
        before = """
            package abc;
            import io.micronaut.core.version.annotation.Version;
            import io.micronaut.http.annotation.Controller;
            import io.micronaut.http.annotation.Get;
            import io.micronaut.runtime.context.scope.Refreshable;
            
            @Controller
            @Refreshable
            @Version("0.1")
            public class MyController extends BaseController {
                @Get
                public String info() {
                    return "system info: ";
                }
            }
        """
    )

    @Disabled("JavaType.Class does not contain method info.  https://github.com/openrewrite/rewrite/issues/150")
    @Test
    fun refreshableMethodOverride() = assertChanged(
        dependsOn = arrayOf(
            """
                package abc;
                import io.micronaut.core.version.annotation.Version;
                
                public class BaseController {
                    @Version("1.0.0")
                    public void doSomething() {
                    }
                }
            """
        ),
        before = """
            package abc;

            public class MyController extends BaseController {
                @Override
                public void doSomething() {
                }
            }
        """,
        after = """
            package abc;
            import io.micronaut.core.version.annotation.Version;
            
            public class MyController extends BaseController {
                @Override
                @Version("1.0.0")
                public void doSomething() {
                }
            }
        """
    )
}