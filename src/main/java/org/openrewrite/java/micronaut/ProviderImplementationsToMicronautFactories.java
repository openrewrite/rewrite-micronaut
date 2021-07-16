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
import org.openrewrite.TreeVisitor;
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
                    "package io.micronaut.context.annotation; @Singleton public @interface Factory {}")
                    .build());
    private static final AnnotationMatcher SINGLETON_ANNOTATION_MATCHER = new AnnotationMatcher("@javax.inject.Singleton");

    @Override
    public String getDisplayName() {
        return "Provider implementation beans to Micronaut Factories";
    }

    @Override
    public String getDescription() {
        return "Micronaut 2.x `javax.inject.Provider` beans automatically created beans for the return type of the get method, Micronaut 3.x uses the `@Factory` to express the same behavior.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("javax.inject.Provider");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                if (classDecl.getImplements() != null
                        && classDecl.getImplements().stream().anyMatch(impl -> TypeUtils.isOfClassType(impl.getType(), "javax.inject.Provider")
                        && classDecl.getLeadingAnnotations().stream().anyMatch(SINGLETON_ANNOTATION_MATCHER::matches))) {
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
            }
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            if (cd.getLeadingAnnotations().stream().anyMatch(SINGLETON_ANNOTATION_MATCHER::matches)) {
                cd = cd.withLeadingAnnotations(ListUtils.map(cd.getLeadingAnnotations(), anno -> {
                    if (SINGLETON_ANNOTATION_MATCHER.matches(anno)) {
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
            MethodMatcher mm = getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).getMessage("provider-get");
            if (mm != null && mm.matches(md.getType())
                    && md.getLeadingAnnotations().stream().noneMatch(SINGLETON_ANNOTATION_MATCHER::matches)) {
                md = md.withTemplate(JavaTemplate.builder(this::getCursor, "@Singleton")
                                .imports("javax.inject.Singleton")
                                .javaParser(JAVA_PARSER::get).build(),
                        md.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                maybeAddImport("javax.inject.Singleton");
            }
            return md;
        }
    }

}
