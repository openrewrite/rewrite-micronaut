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
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CopyNonInheritedAnnotations extends Recipe {

    private static final Set<String> NON_INHERITED_ANNOTATION_TYPES = Stream.of(
            "io.micronaut.aop.Around",
            "io.micronaut.aop.AroundConstruct",
            "io.micronaut.aop.InterceptorBean",
            "io.micronaut.aop.InterceptorBinding",
            "io.micronaut.aop.Introduction",
            "io.micronaut.core.annotation.Creator",
            "io.micronaut.core.annotation.Experimental",
            "io.micronaut.core.annotation.Nullable",
            "io.micronaut.core.annotation.NonNull",
            "io.micronaut.core.annotation.Order",
            "io.micronaut.core.annotation.ReflectiveAccess",
            "io.micronaut.core.annotation.TypeHint",
            "io.micronaut.core.version.annotation.Version",
            "io.micronaut.context.annotation.AliasFor",
            "io.micronaut.context.annotation.Any",
            "io.micronaut.context.annotation.Bean",
            "io.micronaut.context.annotation.ConfigurationBuilder",
            "io.micronaut.context.annotation.ConfigurationInject",
            "io.micronaut.context.annotation.ConfigurationProperties",
            "io.micronaut.context.annotation.ConfigurationReader",
            "io.micronaut.context.annotation.Context",
            "io.micronaut.context.annotation.DefaultScope",
            "io.micronaut.context.annotation.EachBean",
            "io.micronaut.context.annotation.Factory",
            "io.micronaut.context.annotation.NonBinding",
            "io.micronaut.context.annotation.Parallel",
            "io.micronaut.context.annotation.Parameter",
            "io.micronaut.context.annotation.Primary",
            "io.micronaut.context.annotation.Property",
            "io.micronaut.context.annotation.PropertySource",
            "io.micronaut.context.annotation.Prototype",
            "io.micronaut.context.annotation.Replaces",
            "io.micronaut.context.annotation.Requirements",
            "io.micronaut.context.annotation.Requires",
            "io.micronaut.context.annotation.Secondary",
            "io.micronaut.context.annotation.Type",
            "io.micronaut.context.annotation.Value",
            "io.micronaut.context.annotation.Controller",
            "io.micronaut.http.annotation.Filter",
            "io.micronaut.http.annotation.FilterMatcher",
            "io.micronaut.http.client.annotation.Client",
            "io.micronaut.jackson.annotation.JacksonFeatures",
            "io.micronaut.management.endpoint.annotation.EndPont",
            "io.micronaut.management.health.indicator.annotation.Liveness",
            "io.micronaut.management.health.indicator.annotation.Readiness",
            "io.micronaut.messaging.annotation.MessageListener",
            "io.micronaut.messaging.annotation.MessageProducer",
            "io.micronaut.retry.annotation.CircuitBreaker",
            "io.micronaut.retry.annotation.Fallback",
            "io.micronaut.retry.annotation.Recoverable",
            "io.micronaut.retry.annotation.Retryable",
            "io.micronaut.runtime.context.scope.Refreshable",
            "io.micronaut.runtime.context.scope.ScopedProxy",
            "io.micronaut.runtime.context.scope.ThreadLocal",
            "io.micronaut.runtime.http.scope.RequestScope",
            "io.micronaut.scheduling.annotation.Async",
            "io.micronaut.scheduling.annotation.ExecuteOn",
            "io.micronaut.scheduling.annotation.Scheduled",
            "io.micronaut.websocket.annotation.ClientWebSocket",
            "io.micronaut.websocket.annotation.ServerWebSocket",
            "io.micronaut.websocket.annotation.WebSocketComponent"
    ).collect(Collectors.toSet());

    @Override
    public String getDisplayName() {
        return "Copy non-inherited annotations from super class";
    }

    @Override
    public String getDescription() {
        return "As of Micronaut 3.x only [annotations](https://github.com/micronaut-projects/micronaut-core/blob/3.0.x/src/main/docs/guide/appendix/breaks.adoc#annotation-inheritance) that are explicitly meta-annotated with `@Inherited` are inherited from parent classes and interfaces.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getApplicableTest() {
        return new UsesType<>("io.micronaut..*", false);
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Map<String, List<J.Annotation>> parentAnnotationsByType = new HashMap<>();
        JavaIsoVisitor<Map<String, List<J.Annotation>>> parentAnnotationCollector = new org.openrewrite.java.JavaIsoVisitor<Map<String, List<J.Annotation>>>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Map<String, List<J.Annotation>> classAnnos) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, classAnnos);
                if (cd.getType() != null) {
                    String classFqn = cd.getType().getFullyQualifiedName();
                    for (J.Annotation annotation : cd.getLeadingAnnotations()) {
                        JavaType.FullyQualified annoFq = TypeUtils.asFullyQualified(annotation.getType());
                        if (annoFq != null && NON_INHERITED_ANNOTATION_TYPES.stream().anyMatch(fqn -> fqn.equals(annoFq.getFullyQualifiedName()))) {
                            classAnnos.computeIfAbsent(classFqn, v -> new ArrayList<>()).add(annotation);
                        }
                    }
                }
                return cd;
            }
        };
        for (SourceFile sourceFile : before) {
            parentAnnotationCollector.visit(sourceFile, parentAnnotationsByType);
        }

        CopyAnnoVisitor copyAnnoVisitor = new CopyAnnoVisitor(parentAnnotationsByType);
        return ListUtils.map(before, sourceFile -> {
            if (sourceFile instanceof J.CompilationUnit) {
                return (SourceFile) copyAnnoVisitor.visit(sourceFile, ctx);
            }
            return sourceFile;
        });
    }

    private static final class CopyAnnoVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final Map<String, List<J.Annotation>> parentAnnotationsByType;

        private CopyAnnoVisitor(Map<String, List<J.Annotation>> parentAnnotationsByType) {
            this.parentAnnotationsByType = parentAnnotationsByType;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            if (parentAnnotationsByType.isEmpty()) {
                return classDecl;
            }

            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);

            //First collect the names of all super classes and interfaces.
            Set<String> parentTypes = new HashSet<>();
            JavaType.FullyQualified currentFq = cd.getType();

            while (currentFq != null) {
                parentTypes.add(currentFq.getFullyQualifiedName());
                for (JavaType.FullyQualified i : currentFq.getInterfaces()) {
                    parentTypes.add(i.getFullyQualifiedName());
                }
                currentFq = currentFq.getSupertype();
                if (currentFq != null && parentTypes.contains(currentFq.getFullyQualifiedName())) {
                    break;
                }
            }

            //Collect the annotation names already applied to the class.
            Set<String> existingAnnotations = new HashSet<>();
            for (J.Annotation leadingAnnotation : cd.getLeadingAnnotations()) {
                JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(leadingAnnotation.getType());
                if (fullyQualified != null) {
                    existingAnnotations.add(fullyQualified.getFullyQualifiedName());
                }
            }

            List<J.Annotation> annotationsFromParentClass = new ArrayList<>();
            for (String parentTypeFq : parentTypes) {
                List<J.Annotation> parentAnnotations = parentAnnotationsByType.get(parentTypeFq);
                if (parentAnnotations != null) {
                    for (J.Annotation annotation : parentAnnotations) {
                        JavaType.FullyQualified annotationName = TypeUtils.asFullyQualified(annotation.getType());
                        if (annotationName != null && !existingAnnotations.contains(annotationName.getFullyQualifiedName())) {
                            //If the annotation does not exist on the current class, add it.
                            annotationsFromParentClass.add(annotation);
                            existingAnnotations.add(annotationName.getFullyQualifiedName());
                        }
                    }
                }
            }

            List<J.Annotation> afterAnnotationList = ListUtils.concatAll(cd.getLeadingAnnotations(), annotationsFromParentClass);
            if (afterAnnotationList != cd.getLeadingAnnotations()) {
                cd = cd.withLeadingAnnotations(afterAnnotationList);
                cd = autoFormat(cd, cd.getName(), executionContext, getCursor());
                for (J.Annotation annotation : annotationsFromParentClass) {
                    JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(annotation.getType());
                    if (fullyQualified != null) {
                        maybeAddImport(fullyQualified.getFullyQualifiedName());
                    }
                }
            }
            return cd;
        }
    }
}
