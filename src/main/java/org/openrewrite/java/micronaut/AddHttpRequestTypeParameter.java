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

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Iterator;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class AddHttpRequestTypeParameter extends Recipe {

    private static final String IO_MICRONAUT_HTTP_HTTP_REQUEST = "io.micronaut.http.HttpRequest";
    private static final List<String> CANDIDATE_INTERFACES = Arrays.asList(
            "io.micronaut.security.authentication.AuthenticationProvider",
            "io.micronaut.security.token.jwt.validator.GenericJwtClaimsValidator",
            "io.micronaut.security.token.jwt.validator.JwtClaimsValidator",
            "io.micronaut.security.oauth2.endpoint.endsession.response.EndSessionCallbackUrlBuilder",
            "io.micronaut.security.oauth2.url.AbsoluteUrlBuilder",
            "io.micronaut.security.oauth2.url.OauthRouteUrlBuilder",
            "io.micronaut.security.endpoints.introspection.IntrospectionProcessor",
            "io.micronaut.security.filters.AuthenticationFetcher",
            "io.micronaut.security.token.reader.TokenReader",
            "io.micronaut.security.token.reader.TokenResolver",
            "io.micronaut.security.token.validator.TokenValidator");

    @Getter
    final String displayName = "Add `HttpRequest` type parameter for implemented interfaces";

    @Getter
    final String description = "Add an `HttpRequest` type parameter to a class `implements` statement for interfaces that have been " +
            "generically parameterized where they previously specified `HttpRequest` explicitly.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                List<TypeTree> mappedInterfaceTypes = ListUtils.map(c.getImplements(), interfaceType -> {
                    if (interfaceType instanceof J.ParameterizedType) {
                        return interfaceType;
                    }
                    JavaType.FullyQualified fqInterfaceType = TypeUtils.asFullyQualified(interfaceType.getType());
                    if (fqInterfaceType != null && isCandidateInterface(fqInterfaceType)) {
                        if (hasErasedRequestOverride(c, fqInterfaceType)) {
                            return interfaceType;
                        }
                        maybeAddImport(IO_MICRONAUT_HTTP_HTTP_REQUEST);
                        J.ParameterizedType httpRequestParameterized = httpRequestType();
                        NameTree nameTree = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), fqInterfaceType.getClassName(), null, null);
                        return new J.ParameterizedType(Tree.randomId(), interfaceType.getPrefix(), Markers.EMPTY, nameTree,
                                JContainer.build(singletonList(JRightPadded.build(httpRequestParameterized))), fqInterfaceType);
                    }
                    return interfaceType;
                });
                return c.withImplements(mappedInterfaceTypes);
            }

            private boolean hasErasedRequestOverride(J.ClassDeclaration classDeclaration, JavaType.FullyQualified interfaceType) {
                // An Object override already implements the generic API. Narrowing it would break its callers.
                Iterator<JavaType.Method> methods = interfaceType.getVisibleMethods();
                while (methods.hasNext()) {
                    JavaType.Method candidate = methods.next();
                    if (candidate.getParameterTypes().stream().noneMatch(JavaType.GenericTypeVariable.class::isInstance)) {
                        continue;
                    }
                    for (Statement statement : classDeclaration.getBody().getStatements()) {
                        if (statement instanceof J.MethodDeclaration) {
                            J.MethodDeclaration method = (J.MethodDeclaration) statement;
                            if (method.getMethodType() != null && !method.hasModifier(J.Modifier.Type.Static) &&
                                matchesErasedSignature(method.getMethodType(), candidate)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

            private J.ParameterizedType httpRequestType() {
                JavaType.FullyQualified rawType = JavaType.ShallowClass.build(IO_MICRONAUT_HTTP_HTTP_REQUEST);
                JavaType.Parameterized type = new JavaType.Parameterized(null, rawType, singletonList(
                        new JavaType.GenericTypeVariable(null, "?", JavaType.GenericTypeVariable.Variance.INVARIANT, emptyList())));
                J.Identifier name = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "HttpRequest", rawType, null);
                return new J.ParameterizedType(Tree.randomId(), Space.EMPTY, Markers.EMPTY, name,
                        JContainer.build(singletonList(JRightPadded.build(new J.Wildcard(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null)))), type);
            }

            private boolean matchesErasedSignature(JavaType.Method method, JavaType.Method candidate) {
                if (!method.getName().equals(candidate.getName()) ||
                    method.getParameterTypes().size() != candidate.getParameterTypes().size()) {
                    return false;
                }
                for (int i = 0; i < method.getParameterTypes().size(); i++) {
                    JavaType expected = candidate.getParameterTypes().get(i);
                    JavaType actual = method.getParameterTypes().get(i);
                    if (expected instanceof JavaType.GenericTypeVariable) {
                        if (!TypeUtils.isOfClassType(actual, "java.lang.Object")) {
                            return false;
                        }
                    } else if (!TypeUtils.isOfType(expected, actual)) {
                        return false;
                    }
                }
                return true;
            }

            private boolean isCandidateInterface(JavaType.FullyQualified fqInterfaceType) {
                if (CANDIDATE_INTERFACES.contains(fqInterfaceType.getFullyQualifiedName())) {
                    for (JavaType javaType : fqInterfaceType.getTypeParameters()) {
                        if (TypeUtils.isAssignableTo(IO_MICRONAUT_HTTP_HTTP_REQUEST, javaType)) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        };
    }
}
