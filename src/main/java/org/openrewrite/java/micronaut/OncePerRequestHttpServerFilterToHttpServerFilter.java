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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.UUID;

public class OncePerRequestHttpServerFilterToHttpServerFilter extends Recipe {
    private static final String ONCE_PER_REQUEST_FILTER_FQN = "io.micronaut.http.filter.OncePerRequestHttpServerFilter";

    @Override
    public String getDisplayName() {
        return "Convert `OncePerRequestServerFilter` extensions to `HttpServerFilter`";
    }

    @Override
    public String getDescription() {
        return "Starting in Micronaut 3.0 all filters are executed once per request. Directly implement `HttpServerFilter` instead of extending `OncePerRequestHttpServerFilter` and replace any usages of `micronaut.once` attributes with a custom attribute name.";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getApplicableTest() {
        return new UsesType<>("io.micronaut.*");
    }

    @Override
    protected UsesType<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>(ONCE_PER_REQUEST_FILTER_FQN);
    }

    @Override
    protected OncePerRequestHttpServerFilterToHttpServerFilterVisitor getVisitor() {
        return new OncePerRequestHttpServerFilterToHttpServerFilterVisitor();
    }

    private static class OncePerRequestHttpServerFilterToHttpServerFilterVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final MethodMatcher GET_KEY_METHOD = new MethodMatcher(ONCE_PER_REQUEST_FILTER_FQN + " getKey(Class)");

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            if (cd.getExtends() != null && cd.getExtends().getType() != null
                    && TypeUtils.isOfClassType(cd.getExtends().getType(), ONCE_PER_REQUEST_FILTER_FQN)) {
                cd = cd.withExtends(null);
                J.Identifier newImplementsIdentifier = J.Identifier.build(UUID.randomUUID(), Space.format(" "), Markers.EMPTY,
                        "HttpServerFilter", JavaType.buildType("io.micronaut.http.filter.HttpServerFilter"));
                J.Block body = cd.getBody();
                //noinspection ConstantConditions
                cd = maybeAutoFormat(cd, cd.withBody(null).withImplements(ListUtils.concat(cd.getImplements(), newImplementsIdentifier)), executionContext, getCursor());
                cd = cd.withBody(body);
                cd = cd.withModifiers(
                        ListUtils.map(cd.getModifiers(), mod -> mod.getType() == J.Modifier.Type.Private ||
                                mod.getType() == J.Modifier.Type.Protected ?
                                mod.withType(J.Modifier.Type.Public) : mod)
                );
                if (cd.getType() != null) {
                    doAfterVisit(new ChangeMethodName(
                            cd.getType().getFullyQualifiedName() + " doFilterOnce(io.micronaut.http.HttpRequest, io.micronaut.http.filter.ServerFilterChain)",
                            "doFilter"));
                }
                maybeAddImport("io.micronaut.http.filter.HttpServerFilter");
                maybeRemoveImport(ONCE_PER_REQUEST_FILTER_FQN);
            }
            return cd;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            String todoCommentText = "TODO: Replace with custom attribute name";
            if (GET_KEY_METHOD.matches(mi) && mi.getComments().stream().noneMatch(c -> c.getText().equals(todoCommentText))) {
                mi = mi.withComments(ListUtils.concat(mi.getComments(), new Comment(Comment.Style.BLOCK, todoCommentText, " ", Markers.EMPTY)));
            }
            return mi;
        }
    }
}
