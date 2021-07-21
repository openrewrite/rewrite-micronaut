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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Comparator;

public class ProviderImplementationsToMicronautFactories extends Recipe {
    private static final ThreadLocal<JavaParser> JAVA_PARSER = ThreadLocal.withInitial(() ->
            JavaParser.fromJavaVersion().dependsOn(
                    "package javax.inject; public interface Provider<T> {T get();}",
                    "package javax.inject; public @interface Singleton {}",
                    "package jakarta.inject; public interface Provider<T> {T get();}",
                    "package jakarta.inject; public @interface Singleton {}",
                    "package io.micronaut.context.annotation; @Singleton public @interface Factory {}")
                    .build());
    private static final AnnotationMatcher JAVAX_SINGLETON_ANNOTATION_MATCHER = new AnnotationMatcher("@javax.inject.Singleton");
    private static final AnnotationMatcher JAKARTA_SINGLETON_ANNOTATION_MATCHER = new AnnotationMatcher("@jakarta.inject.Singleton");

    private static boolean isSingletonAnnotation(J.Annotation annotation) {
        return JAKARTA_SINGLETON_ANNOTATION_MATCHER.matches(annotation) || JAVAX_SINGLETON_ANNOTATION_MATCHER.matches(annotation);
    }

    @Nullable
    private static String getProviderType(J.ClassDeclaration classDecl) {
        String providerType = null;
        if (classDecl.getImplements() != null) {
            if (classDecl.getImplements().stream().anyMatch(impl -> TypeUtils.isOfClassType(impl.getType(), "javax.inject.Provider"))) {
                providerType = "javax";
            } else if (classDecl.getImplements().stream().anyMatch(impl -> TypeUtils.isOfClassType(impl.getType(), "jakarta.inject.Provider"))) {
                providerType = "jakarta";
            }
        }
        return providerType;
    }

    @Override
    public String getDisplayName() {
        return "Provider implementation beans to Micronaut Factories";
    }

    @Override
    public String getDescription() {
        return "As of Micronaut 3.x the `@Factory` annotation is required for creating beans from `javax.inject.Provider get()` implementations";
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
                doAfterVisit(new UsesType<>("javax.inject.Provider"));
                doAfterVisit(new UsesType<>("jakarta.inject.Provider"));
                return cu;
            }
        };
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                String providerType = getProviderType(classDecl);
                if (providerType != null && classDecl.getLeadingAnnotations().stream().anyMatch(ProviderImplementationsToMicronautFactories::isSingletonAnnotation)) {
                    doAfterVisit(new ProviderImplementationsGenerateFactoriesVisitor());
                }
                return classDecl;
            }
        };
    }

    private static class ProviderImplementationsGenerateFactoriesVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            if (classDecl.getType() != null) {
                getCursor().putMessage("provider-get", new MethodMatcher(classDecl.getType().getFullyQualifiedName() + " get()"));
                String providerType = getProviderType(classDecl);
                if (providerType != null) {
                    getCursor().putMessage("provider-type", providerType);
                }
            }
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            if (cd.getLeadingAnnotations().stream().anyMatch(ProviderImplementationsToMicronautFactories::isSingletonAnnotation)) {
                cd = cd.withLeadingAnnotations(ListUtils.map(cd.getLeadingAnnotations(), anno -> {
                    if (isSingletonAnnotation(anno)) {
                        anno = anno.withTemplate(JavaTemplate.builder(this::getCursor, "@Factory")
                                        .imports("io.micronaut.context.annotation.Factory")
                                        .javaParser(JAVA_PARSER::get).build(),
                                anno.getCoordinates().replace());
                        maybeAddImport("io.micronaut.context.annotation.Factory");
                    }
                    return anno;
                }));
            }
            return cd;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
            Cursor classDeclCursor = getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance);
            MethodMatcher mm = classDeclCursor.getMessage("provider-get");
            String providerType = classDeclCursor.getMessage("provider-type");
            if (mm != null && providerType != null && mm.matches(md.getType())
                    && md.getLeadingAnnotations().stream().noneMatch(ProviderImplementationsToMicronautFactories::isSingletonAnnotation)) {
                md = md.withTemplate(JavaTemplate.builder(this::getCursor, "@Singleton")
                                .imports(providerType + ".inject.Singleton")
                                .javaParser(JAVA_PARSER::get).build(),
                        md.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                maybeAddImport(providerType + ".inject.Singleton");
            }
            return md;
        }
    }

}
