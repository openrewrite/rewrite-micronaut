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

import org.openrewrite.*;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;

public class SubclassesReturnedFromFactoriesNotInjectable extends Recipe {
    private static final AnnotationMatcher FACTORY_ANNOTATION_MATCHER = new AnnotationMatcher("@io.micronaut.context.annotation.Factory");

    @Override
    public String getDisplayName() {
        return "Change factory method return types to reflect their resolved return type";
    }

    @Override
    public String getDescription() {
        return "As of Micronaut 3.x It is no longer possible to inject the internal implementation type from beans produced via factories. Factory method return types are changed to reflect the resolved return type if the method returns a single non-null type that does not match the method declaration return type.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("io.micronaut.context.annotation.Factory", false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                if (cd.getLeadingAnnotations().stream().anyMatch(FACTORY_ANNOTATION_MATCHER::matches)) {
                    doAfterVisit(new FactoryBeansAreTypeVisitor());
                }
                return cd;
            }
        });
    }

    private static class FactoryBeansAreTypeVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final List<AnnotationMatcher> BEAN_ANNOTATION_MATCHERS = Stream.concat(
                        Stream.of("io.micronaut.context.annotation.Context",
                                        "io.micronaut.context.annotation.Prototype",
                                        "io.micronaut.context.annotation.Infrastructure",
                                        "io.micronaut.runtime.context.scope.Refreshable",
                                        "io.micronaut.runtime.http.scope.RequestScope")
                                .map(it -> new AnnotationMatcher("@" + it)),
                        Stream.of("@javax.inject", "@jakarta.inject")
                                .map(it -> new AnnotationMatcher(it + ".Singleton")))
                .map(AnnotationMatcher.class::cast).collect(Collectors.toList());

        private static boolean isBeanAnnotation(J.Annotation annotation) {
            return BEAN_ANNOTATION_MATCHERS.stream().anyMatch(m -> m.matches(annotation));
        }

        @Override
        public J.Return visitReturn(J.Return _return, ExecutionContext executionContext) {
            J.Return rtn = super.visitReturn(_return, executionContext);
            J.MethodDeclaration md = getCursor().firstEnclosing(J.MethodDeclaration.class);
            Expression returnExpression = rtn.getExpression();
            if (md != null && returnExpression != null
                    && !(returnExpression.getType() instanceof JavaType.Primitive)
                    && md.getLeadingAnnotations().stream().anyMatch(FactoryBeansAreTypeVisitor::isBeanAnnotation)) {
                Cursor methodDeclCursor = getCursor().dropParentUntil(J.MethodDeclaration.class::isInstance);
                JavaType returnedType = returnExpression.getType();
                if (returnedType != null) {
                    methodDeclCursor.computeMessageIfAbsent("return-types", v -> new HashSet<>()).add(returnedType);
                }
            }
            return rtn;
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
            Set<JavaType> returnTypes = getCursor().pollMessage("return-types");
            if (returnTypes != null && returnTypes.size() == 1) {
                JavaType returnedType = returnTypes.iterator().next();
                JavaType.FullyQualified methodReturnType = md.getReturnTypeExpression() != null ? TypeUtils.asFullyQualified(md.getReturnTypeExpression().getType()) : null;
                JavaType.FullyQualified returnedTypeFqn = TypeUtils.asFullyQualified(returnedType);
                if (returnedTypeFqn != null && methodReturnType != null && !TypeUtils.isOfType(methodReturnType, returnedType)) {
                    J.Identifier resolvedReturnType = new J.Identifier(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, emptyList(), returnedTypeFqn.getClassName(), returnedType, null);
                    if (returnedType instanceof JavaType.Parameterized && md.getReturnTypeExpression() instanceof J.ParameterizedType) {
                        J.ParameterizedType mdReturnTypeExpression = (J.ParameterizedType) md.getReturnTypeExpression();
                        mdReturnTypeExpression = mdReturnTypeExpression.withClazz(resolvedReturnType);
                        md = maybeAutoFormat(md, md.withReturnTypeExpression(mdReturnTypeExpression), md.getName(), executionContext, getCursor().getParent());
                    } else {
                        md = maybeAutoFormat(md, md.withReturnTypeExpression(resolvedReturnType), md.getName(), executionContext, getCursor().getParent());
                    }
                    maybeRemoveImport(methodReturnType.getFullyQualifiedName());
                }
            }
            return md;
        }
    }
}
