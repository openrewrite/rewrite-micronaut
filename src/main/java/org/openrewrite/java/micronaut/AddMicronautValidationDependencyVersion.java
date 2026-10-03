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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleDependencyConstraint;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.util.*;

public class AddMicronautValidationDependencyVersion extends ScanningRecipe<Set<String>> {
    private static final String GROUP = "io.micronaut.validation";

    @Getter
    final String displayName = "Add missing Micronaut validation dependency versions";
    @Getter
    final String description = "Supply a compatible version for unmanaged Gradle validation dependencies, " +
            "reusing the validation version already in use when possible.";

    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new TreeSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> versions) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    tree.getMarkers().findFirst(GradleProject.class).ifPresent(project -> versions.addAll(versionsIn(project)));
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> versions) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                GradleProject project = tree.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (project == null) {
                    return tree;
                }
                Set<String> localVersions = versionsIn(project);
                Map<String, List<GroupArtifactVersion>> changes = new LinkedHashMap<>();
                JavaSourceFile result = (JavaSourceFile) new GradleDependency.Matcher().groupId(GROUP)
                        .<ExecutionContext>asVisitor((dependency, executionContext) -> {
                            String artifact = dependency.getArtifactId();
                            if (!"micronaut-validation".equals(artifact) && !"micronaut-validation-processor".equals(artifact) ||
                                dependency.getDeclaredVersion() != null ||
                                !dependency.getVersion().isEmpty() ||
                                isManaged(project, dependency.getConfigurationName(), artifact, executionContext)) {
                                return dependency.getTree();
                            }
                            String version = localVersions.size() == 1 ? localVersions.iterator().next() :
                                    versions.size() == 1 ? versions.iterator().next() : null;
                            try {
                                if (version == null) {
                                    version = new DependencyVersionSelector(metadataFailures, project, null)
                                            .select(new GroupArtifact(GROUP, artifact), dependency.getConfigurationName(), "4.x", null, executionContext);
                                }
                            } catch (MavenDownloadingException e) {
                                return e.warn(dependency.getTree());
                            }
                            if (version == null) {
                                return dependency.getTree();
                            }
                            changes.computeIfAbsent(dependency.getConfigurationName(), k -> new ArrayList<>())
                                    .add(new GroupArtifactVersion(GROUP, artifact, version));
                            return dependency.withDeclaredVersion(version).getTree();
                        }).visitNonNull(tree, ctx);
                if (result != tree) {
                    GradleProject updated = project;
                    for (Map.Entry<String, List<GroupArtifactVersion>> change : changes.entrySet()) {
                        for (GroupArtifactVersion gav : change.getValue()) {
                            updated = updated.upgradeDirectDependencyVersion(change.getKey(), gav, ctx);
                        }
                    }
                    result = result.withMarkers(result.getMarkers().setByType(updated));
                }
                return result;
            }
        };
    }

    private static Set<String> versionsIn(GradleProject project) {
        Set<String> versions = new TreeSet<>();
        for (GradleDependencyConfiguration configuration : project.getConfigurations()) {
            for (ResolvedDependency dependency : configuration.getDirectResolvedShallow()) {
                if (GROUP.equals(dependency.getGroupId()) && dependency.getVersion().startsWith("4.")) {
                    versions.add(dependency.getVersion());
                }
            }
        }
        return versions;
    }

    private static boolean isManaged(GradleProject project, String configuration, String artifact, ExecutionContext ctx) {
        if (project.getSpringDependencyManagementPlugin() != null &&
            project.getSpringDependencyManagementPlugin().getManagedVersions().containsKey(GROUP + ":" + artifact)) {
            return true;
        }
        GradleDependencyConfiguration target = project.getConfiguration(configuration);
        if (target == null) {
            return false;
        }
        if (isManaged(project, target, artifact, ctx)) {
            return true;
        }
        // A declaration can be managed on the resolvable classpaths which consume it.
        List<GradleDependencyConfiguration> consumers = new ArrayList<>();
        for (GradleDependencyConfiguration child : project.configurationsExtendingFrom(target, true)) {
            if (child.isCanBeResolved()) {
                consumers.add(child);
            }
        }
        return !consumers.isEmpty() && consumers.stream().allMatch(child -> isManaged(project, child, artifact, ctx));
    }

    private static boolean isManaged(GradleProject project, GradleDependencyConfiguration configuration,
                                     String artifact, ExecutionContext ctx) {
        List<GradleDependencyConfiguration> configurations = new ArrayList<>(configuration.allExtendsFrom());
        configurations.add(configuration);
        for (GradleDependencyConfiguration candidate : configurations) {
            for (GradleDependencyConstraint constraint : candidate.getConstraints()) {
                if (GROUP.equals(constraint.getGroupId()) && artifact.equals(constraint.getArtifactId()) &&
                    !constraint.approximateEffectiveVersion().isEmpty()) {
                    return true;
                }
            }
            if (candidate.getPlatformManagedVersion(new GroupArtifact(GROUP, artifact), project.getMavenRepositories(), ctx) != null) {
                return true;
            }
        }
        return false;
    }
}
