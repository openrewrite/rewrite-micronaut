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

import lombok.Data;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.*;

import java.util.*;

public class TypeRequiresIntrospection extends ScanningRecipe<TypeRequiresIntrospection.Accumulator> {
    private static final Collection<String> typesRequiringIntrospection = Arrays.asList("io.micronaut.http.annotation.Controller", "io.micronaut.http.client.annotation.Client");

    @Override
    public String getDisplayName() {
        return "Add `@Introspected` to classes requiring a map representation";
    }

    @Override
    public String getDescription() {
        return "In Micronaut 2.x a reflection-based strategy was used to retrieve that information if the class was not annotated with `@Introspected`. As of Micronaut 3.x it is required to annotate classes with `@Introspected` that are used in this way.";
    }

    private static boolean parentRequiresIntrospection(@Nullable JavaType.FullyQualified type) {
        if (type == null) {
            return false;
        }
        for (JavaType.FullyQualified fullyQualified : type.getAnnotations()) {
            if (typesRequiringIntrospection.contains(fullyQualified.getFullyQualifiedName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                // look for classes requiring Introspected types
                FindParamsAndReturnTypes findParamsAndReturnTypes = new FindParamsAndReturnTypes();
                if (tree instanceof J.CompilationUnit) {
                    J.CompilationUnit cu = (J.CompilationUnit) tree;
                    for (J.ClassDeclaration classDeclaration : cu.getClasses()) {
                        if (parentRequiresIntrospection(classDeclaration.getType())) {
                            findParamsAndReturnTypes.visit(classDeclaration, acc.getIntrospectableTypes());
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof J.CompilationUnit) {
                    J.CompilationUnit cu = (J.CompilationUnit) tree;
                    for (J.ClassDeclaration aClass : cu.getClasses()) {
                        if (acc.getIntrospectableTypes().contains(aClass.getType())) {
                            return new AddIntrospectionAnnotationVisitor().visit(cu, acc.getIntrospectableTypes());
                        }
                    }
                }
                return tree;
            }
        };
    }

    private static final class FindParamsAndReturnTypes extends JavaIsoVisitor<Set<JavaType.FullyQualified>> {
        private void maybeAddType(@Nullable JavaType.FullyQualified type, Set<JavaType.FullyQualified> foundTypes) {
            if (type != null && !TypeRequiresIntrospection.parentRequiresIntrospection(type)) {
                foundTypes.add(type);
            }
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Set<JavaType.FullyQualified> foundTypes) {
            if (method.isConstructor()) {
                return method;
            }

            // method parameters need introspection
            for (Statement param : method.getParameters()) {
                if (param instanceof J.VariableDeclarations) {
                    J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) param;
                    for (J.VariableDeclarations.NamedVariable namedVariable : variableDeclarations.getVariables()) {
                        if (namedVariable.getType() instanceof JavaType.Parameterized) {
                            for (JavaType type : ((JavaType.Parameterized) namedVariable.getType()).getTypeParameters()) {
                                maybeAddType(TypeUtils.asFullyQualified(type), foundTypes);
                            }
                        } else {
                            maybeAddType(TypeUtils.asFullyQualified(namedVariable.getType()), foundTypes);
                        }
                    }
                }
            }
            // return type needs introspection
            if (method.getReturnTypeExpression() instanceof J.ParameterizedType) {
                J.ParameterizedType parameterizedType = (J.ParameterizedType) method.getReturnTypeExpression();
                if (parameterizedType.getTypeParameters() != null) {
                    for (Expression typeParam : parameterizedType.getTypeParameters()) {
                        maybeAddType(TypeUtils.asFullyQualified(typeParam.getType()), foundTypes);
                    }
                }
            } else if (method.getReturnTypeExpression() != null && method.getReturnTypeExpression().getType() != null) {
                maybeAddType(TypeUtils.asFullyQualified(method.getReturnTypeExpression().getType()), foundTypes);
            }
            return method;
        }
    }

    private static class AddIntrospectionAnnotationVisitor extends JavaIsoVisitor<Set<JavaType.FullyQualified>> {
        final String introspectedAnnotationFqn = "io.micronaut.core.annotation.Introspected";
        final AnnotationMatcher INTROSPECTION_ANNOTATION_MATCHER = new AnnotationMatcher("@" + introspectedAnnotationFqn);
        final JavaTemplate templ = JavaTemplate.builder("@Introspected")
                .imports(introspectedAnnotationFqn)
                .javaParser(JavaParser.fromJavaVersion().dependsOn("package io.micronaut.core.annotation; public @interface Introspected {}"))
                .build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<JavaType.FullyQualified> introspectableTypes) {
            if (!introspectableTypes.contains(TypeUtils.asFullyQualified(classDecl.getType()))) {
                return classDecl;
            }

            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, introspectableTypes);
            if (cd.getLeadingAnnotations().stream().noneMatch(INTROSPECTION_ANNOTATION_MATCHER::matches)) {
                cd = cd.withTemplate(templ, getCursor(), cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                maybeAddImport(introspectedAnnotationFqn);
            }
            return cd;
        }
    }

    @Data
    static class Accumulator {
        Set<JavaType.FullyQualified> introspectableTypes = new HashSet<>();
    }
}
