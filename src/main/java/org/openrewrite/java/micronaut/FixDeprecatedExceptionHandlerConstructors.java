/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class FixDeprecatedExceptionHandlerConstructors extends Recipe {
    private static final List<String> exception_handlers = Arrays.asList(
            "io.micronaut.http.server.netty.converters.DuplicateRouteHandler",
            "io.micronaut.http.server.netty.converters.UnsatisfiedRouteHandler",
            "io.micronaut.http.server.exceptions.ContentLengthExceededHandler",
            "io.micronaut.http.server.exceptions.ConversionErrorHandler",
            "io.micronaut.http.server.exceptions.HttpStatusHandler",
            "io.micronaut.http.server.exceptions.JsonExceptionHandler",
            "io.micronaut.http.server.exceptions.URISyntaxHandler",
            "io.micronaut.http.server.exceptions.UnsatisfiedArgumentHandler",
            "io.micronaut.validation.exceptions.ConstraintExceptionHandler",
            "io.micronaut.validation.exceptions.ValidationExceptionHandler"
    );
    @SuppressWarnings("unchecked")
    private static final TreeVisitor<?, ExecutionContext> precondition =
            Preconditions.or(exception_handlers.stream().map(fqn -> new UsesType<>(fqn, false)).toArray(TreeVisitor[]::new));

    private static final AnnotationMatcher javax_matcher = new AnnotationMatcher("@javax.inject.Inject");
    private static final AnnotationMatcher jakarta_matcher = new AnnotationMatcher("@jakarta.inject.Inject");

    @Override
    public String getDisplayName() {
        return "Fix deprecated no-arg `ExceptionHandler` constructors";
    }

    @Override
    public String getDescription() {
        return "Adds `ErrorResponseProcessor` argument to deprecated no-arg `ExceptionHandler` constructors.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(precondition, Repeat.repeatUntilStable(new JavaIsoVisitor<ExecutionContext>() {

            final JavaParser.Builder<?, ?> parser =
                    JavaParser.fromJavaVersion()
                            .dependsOn("package jakarta.inject; public @interface Inject {}",
                                    "package io.micronaut.http.server.exceptions.response; public interface ErrorContext {}",
                                    "package io.micronaut.http; public interface MutableHttpResponse<B> {}",
                                    "package io.micronaut.http.server.exceptions.response; public interface ErrorResponseProcessor<T> {MutableHttpResponse<T> processResponse(ErrorContext errorContext, MutableHttpResponse<?> baseResponse);}",
                                    "package io.micronaut.validation.exceptions; public class ConstraintExceptionHandler { public ConstraintExceptionHandler(ErrorResponseProcessor<?> responseProcessor){}}");

            final JavaTemplate injectTemplate = JavaTemplate.builder("@Inject")
                    .javaParser(parser)
                    .imports("jakarta.inject.Inject")
                    .build();

            private final String errorResponseProcessorFqn = "io.micronaut.http.server.exceptions.response.ErrorResponseProcessor";

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                J.ClassDeclaration cd = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (cd != null && "super".equals(mi.getSimpleName()) && isClassExceptionHandler(cd)) {
                    if (mi.getArguments().stream().noneMatch(exp -> TypeUtils.isOfClassType(exp.getType(), errorResponseProcessorFqn))) {
                        mi = mi.withArguments(Collections.singletonList(new J.Identifier(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, emptyList(), "errorResponseProcessor", JavaType.buildType(errorResponseProcessorFqn), null)));
                    }
                    if (mi.getArguments().stream().anyMatch(exp -> TypeUtils.isOfClassType(exp.getType(), errorResponseProcessorFqn))) {
                        getCursor().dropParentUntil(J.MethodDeclaration.class::isInstance).putMessage("super-invocation-exists", Boolean.TRUE);
                    }
                }
                return mi;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                J.ClassDeclaration cd = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (cd != null && isClassExceptionHandler(cd)) {
                    if (md.isConstructor()) {
                        getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).putMessage("constructor-exists", Boolean.TRUE);
                        if (md.getLeadingAnnotations().stream().noneMatch(anno -> jakarta_matcher.matches(anno) || javax_matcher.matches(anno))) {
                            md = injectTemplate.apply(new Cursor(getCursor().getParent(), md),
                                    md.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                        }
                        maybeAddImport("jakarta.inject.Inject");

                        if (md.getParameters().stream().noneMatch(this::isErrorProcessorParameter)) {
                            List<Object> params = md.getParameters().stream().filter(j -> !(j instanceof J.Empty)).collect(Collectors.toList());
                            params.add("ErrorResponseProcessor errorResponseProcessor");
                            JavaTemplate paramsTemplate = JavaTemplate.builder(params.stream().map(p -> "#{}").collect(Collectors.joining(", ")))
                                    .contextSensitive()
                                    .imports(errorResponseProcessorFqn)
                                    .javaParser(parser).build();
                            md = paramsTemplate.apply(
                                    new Cursor(getCursor().getParent(), md),
                                    md.getCoordinates().replaceParameters(),
                                    params.toArray());
                        }

                        if (getCursor().pollMessage("super-invocation-exists") == null) {
                            Optional<J.Identifier> errorResponseVar = md.getParameters().stream().filter(J.VariableDeclarations.class::isInstance)
                                    .map(J.VariableDeclarations.class::cast)
                                    .filter(v -> TypeUtils.isOfClassType(v.getType(), errorResponseProcessorFqn))
                                    .map(v -> v.getVariables().get(0).getName()).findFirst();
                            if (errorResponseVar.isPresent() && md.getBody() != null && getCursor().getParent() != null) {
                                JavaTemplate superInvocationTemplate = JavaTemplate.builder("super(#{any(" + errorResponseProcessorFqn + ")});")
                                        .contextSensitive()
                                        .imports(errorResponseProcessorFqn)
                                        .javaParser(parser).build();
                                md = maybeAutoFormat(md, superInvocationTemplate.apply(
                                                getCursor(),
                                                md.getBody().getCoordinates().lastStatement(),
                                                errorResponseVar.get()),
                                        ctx, getCursor().getParent());
                                assert md.getBody() != null;
                                md = md.withBody(moveLastStatementToFirst(md.getBody()));
                            }
                        }
                        maybeAddImport(errorResponseProcessorFqn);
                    }
                }
                return md;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                JavaType.FullyQualified cdFq = cd.getExtends() != null ? TypeUtils.asFullyQualified(cd.getExtends().getType()) : null;
                if (cdFq != null && exception_handlers.stream().anyMatch(fqn -> TypeUtils.isOfClassType(cdFq, fqn))) {
                    if (!Boolean.TRUE.equals(getCursor().pollMessage("constructor-exists"))) {
                        JavaTemplate template = JavaTemplate.builder(
                                        "@Inject\npublic " + cd.getSimpleName() + "(ErrorResponseProcessor errorResponseProcessor) {" +
                                        "super(errorResponseProcessor);}")
                                .contextSensitive()
                                .imports(errorResponseProcessorFqn, "jakarta.inject.Inject", cdFq.getFullyQualifiedName())
                                .javaParser(parser).build();
                        cd = template.apply(getCursor(),
                                cd.getBody().getCoordinates().lastStatement());
                        cd = cd.withBody(moveLastStatementToFirst(cd.getBody()));
                        maybeAddImport("jakarta.inject.Inject");
                    }
                }
                return cd;
            }

            private boolean isErrorProcessorParameter(Statement statement) {
                return statement instanceof J.VariableDeclarations
                       && TypeUtils.isOfClassType(((J.VariableDeclarations) statement).getType(), errorResponseProcessorFqn);
            }

            private boolean isClassExceptionHandler(J.ClassDeclaration cd) {
                JavaType.FullyQualified cdFq = cd.getExtends() != null ? TypeUtils.asFullyQualified(cd.getExtends().getType()) : null;
                return cdFq != null && exception_handlers.stream().anyMatch(fqn -> TypeUtils.isOfClassType(cdFq, fqn));
            }

            private J.Block moveLastStatementToFirst(J.Block block) {
                if (block.getStatements().size() > 1) {
                    List<Statement> statements = block.getStatements();
                    Statement stmt = statements.get(statements.size() - 1);
                    statements.remove(stmt);
                    statements.add(0, stmt);
                    block = block.withStatements(statements);
                }
                return block;
            }
        }));
    }
}
