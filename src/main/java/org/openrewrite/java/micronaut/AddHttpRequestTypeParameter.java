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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class AddHttpRequestTypeParameter extends Recipe {

    private static final Pattern HTTP_REQUEST_TYPE = Pattern.compile("io.micronaut.http.HttpRequest");
    private static final List<String> CANDIDATE_INTERFACES;

    static {
        CANDIDATE_INTERFACES = new ArrayList<>();
        CANDIDATE_INTERFACES.add("io.micronaut.security.authentication.AuthenticationProvider");
        CANDIDATE_INTERFACES.add("io.micronaut.security.token.jwt.validator.GenericJwtClaimsValidator");
        CANDIDATE_INTERFACES.add("io.micronaut.security.token.jwt.validator.JwtClaimsValidator");
        CANDIDATE_INTERFACES.add("io.micronaut.security.oauth2.endpoint.endsession.response.EndSessionCallbackUrlBuilder");
        CANDIDATE_INTERFACES.add("io.micronaut.security.oauth2.url.AbsoluteUrlBuilder");
        CANDIDATE_INTERFACES.add("io.micronaut.security.oauth2.url.OauthRouteUrlBuilder");
        CANDIDATE_INTERFACES.add("io.micronaut.security.endpoints.introspection.IntrospectionProcessor");
        CANDIDATE_INTERFACES.add("io.micronaut.security.filters.AuthenticationFetcher");
        CANDIDATE_INTERFACES.add("io.micronaut.security.token.reader.TokenReader");
        CANDIDATE_INTERFACES.add("io.micronaut.security.token.reader.TokenResolver");
        CANDIDATE_INTERFACES.add("io.micronaut.security.token.validator.TokenValidator");

        /*
        CANDIDATE_INTERFACES.add("io.micronaut.security.errors.PriorToLoginPersistence"); //TODO - This one also has a generic parameter for the HttpResponse
        CANDIDATE_INTERFACES.add("io.micronaut.security.handlers.LoginHandler"); //TODO - This one also has a generic return type parameter
        CANDIDATE_INTERFACES.add("io.micronaut.security.handlers.LogoutHandler"); //TODO - This one also has a generic return type parameter
        CANDIDATE_INTERFACES.add("io.micronaut.security.handlers.RedirectingLoginHandler"); //TODO - This one also has a generic return type parameter
        CANDIDATE_INTERFACES.add("io.micronaut.security.rules.SecurityRule"); //TODO - This one has a method parameter that must be removed
        CANDIDATE_INTERFACES.add("io.micronaut.views.ViewsRenderer"); //TODO - The request was added as a second parameter here
        */
    }

    @Override
    public String getDisplayName() {
        return "Add HttpRequest type parameter for implemented interfaces";
    }

    @Override
    public String getDescription() {
        return "This recipe adds an HttpRequest type parameter to a class implements statement for interfaces that have been generically parameterized " +
               "where they previously specified HttpRequest explicitly.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, executionContext);
                if (c.getImplements() != null) {
                    List<TypeTree> newInterfaceTypes = ListUtils.map(c.getImplements(), interfaceType -> {
                        JavaType.FullyQualified fqInterfaceType = (JavaType.FullyQualified) interfaceType.getType();
                        if (fqInterfaceType != null && isCandidateInterface(fqInterfaceType)) {
                            JavaType httpRequestType = JavaType.buildType("io.micronaut.http.HttpRequest");
                            J.Identifier httpRequestIdentifier = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, "HttpRequest", httpRequestType, null);
                            J.ParameterizedType httpRequestParameterized = new J.ParameterizedType(Tree.randomId(), Space.EMPTY, Markers.EMPTY, httpRequestIdentifier,
                                    JContainer.build(Collections.singletonList(JRightPadded.build(new J.Wildcard(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null)))), httpRequestType);
                            NameTree nameTree = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, fqInterfaceType.getClassName(), null, null);
                            return new J.ParameterizedType(Tree.randomId(), Space.EMPTY, Markers.EMPTY, nameTree,
                                    JContainer.build(Collections.singletonList(JRightPadded.build(httpRequestParameterized))), fqInterfaceType)
                                    .withPrefix(interfaceType.getPrefix());
                        }
                        return interfaceType;
                    });
                    return c.withImplements(newInterfaceTypes);
                }
                return c;
            }

            private boolean isCandidateInterface(JavaType.FullyQualified fqInterfaceType) {
                return CANDIDATE_INTERFACES.contains(fqInterfaceType.getFullyQualifiedName())
                       && fqInterfaceType.getTypeParameters().stream()
                               .noneMatch(javaType -> javaType.isAssignableFrom(HTTP_REQUEST_TYPE));
            }
        };
    }
}
