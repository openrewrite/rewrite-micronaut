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
                    JavaType.FullyQualified fqInterfaceType = (JavaType.FullyQualified) interfaceType.getType();
                    if (fqInterfaceType != null && isCandidateInterface(fqInterfaceType)) {
                        JavaType httpRequestType = JavaType.buildType(IO_MICRONAUT_HTTP_HTTP_REQUEST);
                        J.Identifier httpRequestIdentifier = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "HttpRequest", httpRequestType, null);
                        J.ParameterizedType httpRequestParameterized = new J.ParameterizedType(Tree.randomId(), Space.EMPTY, Markers.EMPTY, httpRequestIdentifier,
                                JContainer.build(singletonList(JRightPadded.build(new J.Wildcard(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null)))), httpRequestType);
                        NameTree nameTree = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), fqInterfaceType.getClassName(), null, null);
                        return new J.ParameterizedType(Tree.randomId(), interfaceType.getPrefix(), Markers.EMPTY, nameTree,
                                JContainer.build(singletonList(JRightPadded.build(httpRequestParameterized))), fqInterfaceType);
                    }
                    return interfaceType;
                });
                return c.withImplements(mappedInterfaceTypes);
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
