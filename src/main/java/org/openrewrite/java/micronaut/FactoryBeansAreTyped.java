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
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FactoryBeansAreTyped extends Recipe {
    private static final AnnotationMatcher FACTORY_ANNOTATION_MATCHER = new AnnotationMatcher("@io.micronaut.context.annotation.Factory");

    private static final ThreadLocal<JavaParser> JAVA_PARSER = ThreadLocal.withInitial(() ->
            JavaParser.fromJavaVersion().dependsOn(
                            "package javax.inject; public @interface Singleton {}",
                            "package jakarta.inject; public @interface Singleton {}",
                            "package io.micronaut.context.annotation; public @interface Bean { Class<?>[] typed() default {}; }")
                    .build());

    @Override
    public String getDisplayName() {
        return "Add typed bean annotation to beans produced by factories";
    }

    @Override
    public String getDescription() {
        return "As of Micronaut 3.x it is no longer possible to inject the internal implementation type from beans produced via factories. The behavior is restored by using the new `typed` member of the `@Bean` annotation.";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getApplicableTest() {
        return new UsesType<>("io.micronaut.*");
    }

    @Override
    protected UsesType<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("io.micronaut.context.annotation.Factory");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                if (cd.getLeadingAnnotations().stream().anyMatch(FACTORY_ANNOTATION_MATCHER::matches)) {
                    doAfterVisit(new FactoryBeansAreTypeVisitor());
                }
                return cd;
            }
        };
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

        private static final AnnotationMatcher BEAN_ANNOTATION_MATCHER = new AnnotationMatcher("@io.micronaut.context.annotation.Bean");

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
                if (returnedType instanceof JavaType.Method) {
                    JavaType.Method methodType = (JavaType.Method)returnedType;
                    //noinspection AssignmentToNull
                    returnedType = methodType.getResolvedSignature() != null ? methodType.getResolvedSignature().getReturnType() : null;
                }
                if (returnedType != null) {
                    methodDeclCursor.putMessage("returned-type", returnedType);
                }
            }
            return rtn;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
            JavaType returnType = getCursor().pollMessage("returned-type");
            if (returnType != null) {
                JavaType.FullyQualified fqn = md.getReturnTypeExpression() != null ? TypeUtils.asFullyQualified(md.getReturnTypeExpression().getType()) : null;
                JavaType.FullyQualified fqn2 = TypeUtils.asFullyQualified(returnType);

                if (fqn != null && fqn2 != null && !fqn.getFullyQualifiedName().equals(fqn2.getFullyQualifiedName())) {
                    String beanText = "Bean(typed = {" + fqn.getClassName() + ".class, " + fqn2.getClassName() + ".class})";
                    JavaTemplate t = JavaTemplate.builder(this::getCursor,
                                    "@" + beanText).javaParser(JAVA_PARSER::get)
                            .imports("io.micronaut.context.annotation.Bean")
                            .build();

                    if (md.getLeadingAnnotations().stream().noneMatch(BEAN_ANNOTATION_MATCHER::matches)) {
                        md = md.withTemplate(t, md.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                        maybeAddImport("io.micronaut.context.annotation.Bean");
                    } else {
                        md = md.withLeadingAnnotations(ListUtils.map(md.getLeadingAnnotations(), anno -> {
                            if (BEAN_ANNOTATION_MATCHER.matches(anno) && anno.getArguments() != null && anno.getArguments().isEmpty()) {
                                anno = anno.withTemplate(t, anno.getCoordinates().replace());
                            }
                            return anno;
                        }));
                    }
                }
            }
            return md;
        }
    }
}
