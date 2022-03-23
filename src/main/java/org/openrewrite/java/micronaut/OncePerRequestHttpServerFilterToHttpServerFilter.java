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

import java.time.Duration;
import java.util.UUID;

public class OncePerRequestHttpServerFilterToHttpServerFilter extends Recipe {
    private static final String oncePerRequestHttpServerFilterFqn = "io.micronaut.http.filter.OncePerRequestHttpServerFilter";

    @Override
    public String getDisplayName() {
        return "Convert `OncePerRequestServerFilter` extensions to `HttpServerFilter`";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public String getDescription() {
        return "Starting in Micronaut 3.0 all filters are executed once per request. Directly implement `HttpServerFilter` instead of extending `OncePerRequestHttpServerFilter` and replace any usages of `micronaut.once` attributes with a custom attribute name.";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getApplicableTest() {
        return new UsesType<>("io.micronaut..*");
    }

    @Override
    protected UsesType<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>(oncePerRequestHttpServerFilterFqn);
    }

    @Override
    protected OncePerRequestHttpServerFilterToHttpServerFilterVisitor getVisitor() {
        return new OncePerRequestHttpServerFilterToHttpServerFilterVisitor();
    }

    private static class OncePerRequestHttpServerFilterToHttpServerFilterVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final MethodMatcher keyMethodMatcher = new MethodMatcher(oncePerRequestHttpServerFilterFqn + " getKey(Class)");
        private static final MethodMatcher doFilterOnceMethodMatcher = new MethodMatcher("* doFilterOnce(io.micronaut.http.HttpRequest, io.micronaut.http.filter.ServerFilterChain)");

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            if (cd.getExtends() != null && cd.getExtends().getType() != null
                    && TypeUtils.isOfClassType(cd.getExtends().getType(), oncePerRequestHttpServerFilterFqn)) {
                cd = cd.withExtends(null);
                J.Identifier newImplementsIdentifier = new J.Identifier(UUID.randomUUID(), Space.format(" "), Markers.EMPTY,
                        "HttpServerFilter", JavaType.buildType("io.micronaut.http.filter.HttpServerFilter"), null);
                J.Block body = cd.getBody();
                //noinspection ConstantConditions
                cd = maybeAutoFormat(cd, cd.withBody(null).withImplements(ListUtils.concat(cd.getImplements(), newImplementsIdentifier)), executionContext, getCursor());
                cd = cd.withBody(body);
                if (cd.getType() != null) {
                    doAfterVisit(new ChangeMethodName(
                            cd.getType().getFullyQualifiedName() + " doFilterOnce(io.micronaut.http.HttpRequest, io.micronaut.http.filter.ServerFilterChain)",
                            "doFilter", true, false));
                }
                maybeAddImport("io.micronaut.http.filter.HttpServerFilter");
                maybeRemoveImport(oncePerRequestHttpServerFilterFqn);
            }
            return cd;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            J.MethodDeclaration methodDeclaration = super.visitMethodDeclaration(method, executionContext);
            J.ClassDeclaration classDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
            if (classDeclaration != null && doFilterOnceMethodMatcher.matches(methodDeclaration, classDeclaration)) {
                methodDeclaration = methodDeclaration.withModifiers(
                        ListUtils.map(methodDeclaration.getModifiers(), mod -> mod.getType() == J.Modifier.Type.Private ||
                                mod.getType() == J.Modifier.Type.Protected ?
                                mod.withType(J.Modifier.Type.Public) : mod)
                );
            }
            return methodDeclaration;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            String todoCommentText = "TODO: See `Server Filter Behavior` in https://docs.micronaut.io/3.0.x/guide/#breaks for details";
            if (keyMethodMatcher.matches(mi) && mi.getComments().stream().noneMatch(c -> c instanceof TextComment && ((TextComment) c).getText().equals(todoCommentText))) {
                mi = mi.withComments(ListUtils.concat(mi.getComments(), new TextComment(true, todoCommentText, " ", Markers.EMPTY)));
            }
            return mi;
        }
    }
}
