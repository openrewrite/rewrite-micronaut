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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.marker.JavaSearchResult;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Marker;

import java.util.*;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

public class TypeRequiresIntrospection extends Recipe {
    private static final JavaType.FullyQualified INTROSPECTED_ANNOTATION = JavaType.Class.build("io.micronaut.core.annotation.Introspected");
    @SuppressWarnings("ConstantConditions")
    private static final Marker FOUND_REQUIRES_INTROSPECTION_TYPE = new JavaSearchResult(randomId(), null, null);
    private static final String CONTEXT_KEY = "classes-need-introspection";

    @Override
    public String getDisplayName() {
        return "Add `@Introspected` to classes requiring a map representation";
    }

    @Override
    public String getDescription() {
        return "In Micronaut 2.x a reflection-based strategy was used to retrieve that information if the class was not annotated with `@Introspected`. As of Micronaut 3.x it is required to annotate classes with `@Introspected` that are used in this way.";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getApplicableTest() {
        return new UsesType<>("io.micronaut.*");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                Set<JavaType.Class> classesToUpdate = executionContext.getMessage(CONTEXT_KEY);
                if (classesToUpdate != null && classesToUpdate.stream()
                        .anyMatch(jc -> cu.getClasses().stream().map(J.ClassDeclaration::getType).anyMatch(jc::isAssignableFrom))) {
                    return cu.withMarkers(cu.getMarkers().addIfAbsent(FOUND_REQUIRES_INTROSPECTION_TYPE));
                } else {
                    doAfterVisit(new UsesType<>("io.micronaut.http.annotation.Controller"));
                    doAfterVisit(new UsesType<>("io.micronaut.http.client.annotation.Client"));
                }
                return cu;
            }
        };
    }

    @Override
    protected RequiresIntrospectionVisitor getVisitor() {
        return new RequiresIntrospectionVisitor();
    }

    private static class RequiresIntrospectionVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final List<String> REQUIRES_ANNOTATION_TYPES = Arrays.asList("io.micronaut.http.annotation.Controller","io.micronaut.http.client.annotation.Client");
        private static final List<AnnotationMatcher> REQUIRES_INTROSPECTED_ANNOTATION_MATCHERS = REQUIRES_ANNOTATION_TYPES.stream()
                .map(fqn -> new AnnotationMatcher("@" + fqn))
                .collect(Collectors.toList());

        private static boolean classRequiresIntrospectedTypes(J.ClassDeclaration cd) {
            return cd.getLeadingAnnotations().stream().anyMatch(anno -> REQUIRES_INTROSPECTED_ANNOTATION_MATCHERS.stream().anyMatch(p -> p.matches(anno)));
        }

        private static void checkForIntrospectedAnnotation(@Nullable JavaType jc, ExecutionContext executionContext) {
            if (jc instanceof JavaType.Class) {
                JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(jc);
                if (fullyQualified != null && fullyQualified.getAnnotations().stream()
                        .noneMatch(annoFqn -> REQUIRES_ANNOTATION_TYPES.stream().anyMatch(fqn -> TypeUtils.isOfClassType(annoFqn, fqn)))) {
                    executionContext.putMessageInSet(CONTEXT_KEY, jc);
                }
            }
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            if (classRequiresIntrospectedTypes(cd)) {
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                        J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
                        if (!md.isConstructor()) {
                            // method parameters need introspection
                            md.getParameters().stream()
                                    .filter(J.VariableDeclarations.class::isInstance)
                                    .map(j -> ((J.VariableDeclarations) j).getVariables())
                                    .flatMap(List::stream)
                                    .map(J.VariableDeclarations.NamedVariable::getType)
                                    .forEach(jt -> checkForIntrospectedAnnotation(jt,executionContext));

                            // return type needs introspection
                            if (md.getReturnTypeExpression() instanceof J.ParameterizedType) {
                                J.ParameterizedType parameterizedType = (J.ParameterizedType) md.getReturnTypeExpression();
                                if (parameterizedType.getTypeParameters() != null) {
                                    parameterizedType.getTypeParameters().forEach(jt -> checkForIntrospectedAnnotation(jt.getType(), executionContext));
                                }
                            } else if (md.getReturnTypeExpression() != null && md.getReturnTypeExpression().getType() != null) {
                                checkForIntrospectedAnnotation(md.getReturnTypeExpression().getType(), executionContext);
                            }
                        }
                        return md;
                    }
                }.visit(cd, executionContext, getCursor());
            }
            if (executionContext.getMessage(CONTEXT_KEY) != null) {
                doAfterVisit(new AddIntrospectionRecipe());
            }
            return cd;
        }
    }

    private static class AddIntrospectionRecipe extends Recipe {
        private static final AnnotationMatcher INTROSPECTION_ANNOTATION_MATCHER = new AnnotationMatcher("@io.micronaut.core.annotation.Introspected");
        private static final ThreadLocal<JavaParser> JAVA_PARSER = ThreadLocal.withInitial(() ->
                JavaParser.fromJavaVersion().dependsOn(
                        "package io.micronaut.core.annotation; public @interface Introspected {}")
                        .build());

        @Override
        public String getDisplayName() {
            return "Adding Introspection annotation";
        }

        @Override
        protected JavaIsoVisitor<ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {
                final JavaTemplate templ = JavaTemplate.builder(this::getCursor, "@Introspected")
                        .imports(INTROSPECTED_ANNOTATION.getFullyQualifiedName())
                        .javaParser(JAVA_PARSER::get).build();

                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                    J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                    if (cd.getLeadingAnnotations().stream().noneMatch(INTROSPECTION_ANNOTATION_MATCHER::matches)) {
                        Set<JavaType.Class> needsAnnotation = executionContext.getMessage(CONTEXT_KEY);
                        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(cd.getType());
                        if (fullyQualified != null && needsAnnotation != null && needsAnnotation.stream().anyMatch(jc -> TypeUtils.isOfClassType(jc, fullyQualified.getFullyQualifiedName()))) {
                            cd = cd.withTemplate(templ, cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                            maybeAddImport(INTROSPECTED_ANNOTATION.getFullyQualifiedName());
                        }
                    }
                    return cd;
                }
            };
        }
    }
}
