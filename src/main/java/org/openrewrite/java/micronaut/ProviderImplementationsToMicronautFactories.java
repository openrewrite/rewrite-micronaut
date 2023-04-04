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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProviderImplementationsToMicronautFactories extends Recipe {
    private static final ThreadLocal<JavaParser> JAVA_PARSER = ThreadLocal.withInitial(() ->
            JavaParser.fromJavaVersion().dependsOn("package io.micronaut.context.annotation; public @interface Factory {}").build());

    private static final List<AnnotationMatcher> BEAN_ANNOTATION_MATCHERS = Stream.concat(
            Stream.of("io.micronaut.context.annotation.Bean",
                    "io.micronaut.context.annotation.Context",
                    "io.micronaut.context.annotation.Prototype",
                    "io.micronaut.context.annotation.Infrastructure",
                    "io.micronaut.runtime.context.scope.Refreshable",
                    "io.micronaut.runtime.context.scope.ThreadLocal",
                    "io.micronaut.runtime.http.scope.RequestScope")
                    .map(it -> new AnnotationMatcher("@" + it)),
            Stream.of("@javax.inject", "@jakarta.inject")
                    .map(it -> new AnnotationMatcher(it + ".Singleton")))
            .map(AnnotationMatcher.class::cast).collect(Collectors.toList());

    @Override
    public String getDisplayName() {
        return "`Provider` implementation beans to Micronaut `@Factory`";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public String getDescription() {
        return "As of Micronaut 3.x the `@Factory` annotation is required for creating beans from `javax.inject.Provider get()` implementations.";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getApplicableTest() {
        return new UsesType<>("io.micronaut..*", false);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesType<>("javax.inject.Provider", false));
                doAfterVisit(new UsesType<>("jakarta.inject.Provider", false));
                return cu;
            }
        };
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                if (cu.getClasses().stream().anyMatch(cd -> isProvider(cd) && cd.getLeadingAnnotations().stream().anyMatch(ProviderImplementationsToMicronautFactories::isBeanAnnotation))) {
                    doAfterVisit(new ProviderImplementationsGenerateFactoriesVisitor());
                }
                return cu;
            }
        };
    }

    private static boolean isBeanAnnotation(J.Annotation annotation) {
        return BEAN_ANNOTATION_MATCHERS.stream().anyMatch(annotationMatcher -> annotationMatcher.matches(annotation));
    }

    private static boolean isProvider(J.ClassDeclaration classDecl) {
        return classDecl.getType() != null && classDecl.getImplements() != null
                && (classDecl.getImplements().stream().anyMatch(impl -> TypeUtils.isOfClassType(impl.getType(), "javax.inject.Provider"))
                || classDecl.getImplements().stream().anyMatch(impl -> TypeUtils.isOfClassType(impl.getType(), "jakarta.inject.Provider")));
    }

    private static class ProviderImplementationsGenerateFactoriesVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            List<J.Annotation> beanAnnotations = classDecl.getLeadingAnnotations().stream().filter(ProviderImplementationsToMicronautFactories::isBeanAnnotation).collect(Collectors.toList());
            if (classDecl.getType() == null || !isProvider(classDecl) || beanAnnotations.isEmpty()) {
                return classDecl;
            }
            getCursor().putMessage("provider-get", new MethodMatcher(classDecl.getType().getFullyQualifiedName() + " get()"));
            getCursor().putMessage("class-bean-annotations", beanAnnotations);
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);

            cd = cd.withLeadingAnnotations(ListUtils.map(cd.getLeadingAnnotations(), anno -> {
                if (isBeanAnnotation(anno)) {
                    return null;
                }
                return anno;
            }));

            cd = cd.withTemplate(JavaTemplate.builder(this::getCursor, "@Factory")
                            .imports("io.micronaut.context.annotation.Factory")
                            .javaParser(JAVA_PARSER::get).build(),
                    cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
            maybeAddImport("io.micronaut.context.annotation.Factory");
            return cd;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
            Cursor classDeclCursor = getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance);
            MethodMatcher mm = classDeclCursor.getMessage("provider-get");
            List<J.Annotation> beanAnnotations = classDeclCursor.getMessage("class-bean-annotations");
            if (mm != null && mm.matches(md.getMethodType()) && beanAnnotations != null) {
                List<J.Annotation> newBeanAnnotations = beanAnnotations.stream().filter(anno -> !annotationExists(method.getLeadingAnnotations(), anno)).collect(Collectors.toList());
                if (!newBeanAnnotations.isEmpty()) {
                    //noinspection ConstantConditions
                    md = maybeAutoFormat(md, md.withLeadingAnnotations(ListUtils.concatAll(md.getLeadingAnnotations(), newBeanAnnotations)), executionContext, getCursor().getParent());
                }
            }
            return md;
        }

        private static boolean annotationExists(List<J.Annotation> annotations, J.Annotation annotation) {
            return annotations.stream().anyMatch(anno -> {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(anno.getType());
                return fq != null && fq.isAssignableFrom(TypeUtils.asFullyQualified(annotation.getType()));
            });
        }
    }
}
