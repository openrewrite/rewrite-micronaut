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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CopyNonInheritedAnnotationsFromSuperClass extends Recipe {

    private static final Set<String> NON_INHERITED_ANNOTATIONS = Stream.of(
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
        return "As of Micronaut 3.x only [annotations] that are explicitly meta-annotated with `@Inherited`(https://github.com/micronaut-projects/micronaut-core/blob/3.0.x/src/main/docs/guide/appendix/breaks.adoc#annotation-inheritance) are inherited from parent classes and interfaces.";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getApplicableTest() {
        return new UsesType<>("io.micronaut.*");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                List<TypeTree> parentTypes = new ArrayList<>();
                if (cd.getExtends() != null && cd.getExtends().getType() != null) {
                    parentTypes.add(cd.getExtends());
                }
                if (cd.getImplements() != null) {
                    parentTypes.addAll(cd.getImplements());
                }
                List<J.Annotation> newAnnos = calculatePossibleNewAnnotations(cd.getLeadingAnnotations(), parentTypes);
                if (!newAnnos.isEmpty()) {
                    cd = maybeAutoFormat(cd, cd.withLeadingAnnotations(ListUtils.concatAll(cd.getLeadingAnnotations(), newAnnos)), cd, executionContext, getCursor());
                }
                return cd;
            }

            private List<J.Annotation> calculatePossibleNewAnnotations(List<J.Annotation> existingAnnotations, List<TypeTree> parentTypes) {
                return parentTypes.stream()
                        .filter(Objects::nonNull)
                        .map(TypeTree::getType)
                        .filter(Objects::nonNull)
                        .map(JavaType.Class.class::cast)
                        .map(JavaType.Class::getAnnotations)
                        .flatMap(List::stream)
                        .map(JavaType.FullyQualified::getFullyQualifiedName)
                        .filter(NON_INHERITED_ANNOTATIONS::contains)
                        .filter(cda -> CopyNonInheritedAnnotationsFromSuperClass.annotationExists(existingAnnotations, cda))
                        .map(fq -> {
                            maybeAddImport(fq);
                            String name = fq.substring(fq.lastIndexOf(".") + 1);
                            J.Identifier ident = J.Identifier.build(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, name, JavaType.buildType(fq));
                            return new J.Annotation(UUID.randomUUID(), Space.EMPTY, Markers.EMPTY, ident, null);
                        })
                        .collect(Collectors.toList());
            }
        };
    }

    private static boolean annotationExists(List<J.Annotation> annotations, String fullyQualifiedName) {
        return annotations.stream()
                .map(J.Annotation::getType)
                .filter(type -> type instanceof JavaType.FullyQualified)
                .map(JavaType.FullyQualified.class::cast)
                .noneMatch(fq -> fq.getFullyQualifiedName().equals(fullyQualifiedName));
    }
}
