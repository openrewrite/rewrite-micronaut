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
import org.openrewrite.marker.Marker;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.openrewrite.Tree.randomId;

public class TypeRequiresIntrospection extends Recipe {
    private static final JavaType.FullyQualified INTROSPECTED_ANNOTATION = JavaType.Class.build("io.micronaut.core.annotation.Introspected");
    @SuppressWarnings("ConstantConditions")
    private static final Marker FOUND_CHANGE_TO_MAKE = new JavaSearchResult(randomId(), null, null);
    private static final String CONTEXT_KEY = "classes-need-introspection";

    @Override
    public String getDisplayName() {
        return "Add @Introspection annotation to classes requiring a map representation";
    }

    @Override
    public String getDescription() {
        return "In several places in Micronaut, it is required to get a map representation of your object. In previous versions, a reflection based strategy was used to retrieve that information if the class was not annotated with `@introspected`. That functionality has been removed and it is now required to annotate classes with `@introspected` that are being used in this way. Any class may be affected if it is passed as an argument or returned from any controller or client, among other use cases.";
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
                    return cu.withMarkers(cu.getMarkers().addIfAbsent(FOUND_CHANGE_TO_MAKE));
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
        private static final List<Predicate<J.Annotation>> REQUIRES_INTROSPECTED_CLASSES_PREDICATES = Arrays.asList(
                new AnnotationMatcher("@io.micronaut.http.annotation.Controller")::matches,
                new AnnotationMatcher("@io.micronaut.http.client.annotation.Client")::matches);

        private static boolean requiresIntrospectedTypes(J.ClassDeclaration cd) {
            return cd.getLeadingAnnotations().stream().anyMatch(anno -> REQUIRES_INTROSPECTED_CLASSES_PREDICATES.stream().anyMatch(p -> p.test(anno)));
        }

        private static boolean needsIntrospectedAnnotation(@Nullable JavaType.Class jc) {
            return jc != null && jc.getAnnotations().stream().noneMatch(fq -> fq.isAssignableFrom(INTROSPECTED_ANNOTATION));
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            if (requiresIntrospectedTypes(cd)) {
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                        J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
                        // method parameters need introspection
                        md.getParameters().stream()
                                .filter(J.VariableDeclarations.class::isInstance)
                                .map(j -> ((J.VariableDeclarations) j).getVariables())
                                .flatMap(List::stream)
                                .map(J.VariableDeclarations.NamedVariable::getType)
                                .filter(JavaType.Class.class::isInstance)
                                .map(JavaType.Class.class::cast)
                                .filter(RequiresIntrospectionVisitor::needsIntrospectedAnnotation)
                                .forEach(jc -> executionContext.putMessageInSet(CONTEXT_KEY, jc));
                        // return type needs introspection
                        if (md.getReturnTypeExpression() instanceof J.Identifier) {
                            J.Identifier ident = (J.Identifier) md.getReturnTypeExpression();
                            JavaType.Class jc = ident.getType() != null && ident.getType() instanceof JavaType.Class ? (JavaType.Class) ident.getType() : null;
                            if (needsIntrospectedAnnotation(jc)) {
                                executionContext.putMessageInSet(CONTEXT_KEY, jc);
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
                    Set<JavaType.Class> needsAnnotation = executionContext.getMessage(CONTEXT_KEY);
                    if (needsAnnotation != null && needsAnnotation.stream().anyMatch(jc -> jc.isAssignableFrom(classDecl.getType()))) {
                        cd = cd.withTemplate(templ, cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                        maybeAddImport(INTROSPECTED_ANNOTATION.getFullyQualifiedName());
                    }
                    return cd;
                }
            };
        }
    }
}
