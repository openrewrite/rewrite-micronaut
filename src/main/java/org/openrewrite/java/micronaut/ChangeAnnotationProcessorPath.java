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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.xml.AddOrUpdateChild;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;
import static org.openrewrite.xml.MapTagChildrenVisitor.mapTagChildren;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeAnnotationProcessorPath extends Recipe {

    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. The groupId is the first part of a plugin coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. The artifactId is the second part of a plugin coordinate 'com.google.guava:guava:VERSION'. Supports glob expressions.",
            example = "my-deprecated-annotation-processor")
    String oldArtifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use. Defaults to the existing group id.",
            example = "corp.internal.openrewrite.recipe",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use. Defaults to the existing artifact id.",
            example = "my-new-annotation-processor",
            required = false)
    @Nullable
    String newArtifactId;

    @Option(displayName = "New version",
            description = "An version string for the annotation processor path. Version strings that start with 'micronaut.' will be treated specially. ",
            example = "micronaut.validation",
            required = false)
    @Nullable
    String newVersion;

    @Option(displayName = "Exclusions",
            description = "A list of exclusions to apply to the annotation processor path in the format groupId:artifactId",
            example = "io.micronaut:micronaut-inject",
            required = false)
    @Nullable
    List<String> exclusions;

    @Override
    public String getDisplayName() {
        return "Change Maven annotation processor path";
    }

    @Override
    public String getDescription() {
        return "Change the groupId, artifactId, and version of a Maven annotation processor path.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new MavenVisitor<ExecutionContext>() {

            final DependencyMatcher depMatcher = Objects.requireNonNull(DependencyMatcher.build(ChangeAnnotationProcessorPath.this.oldGroupId + ":" + ChangeAnnotationProcessorPath.this.oldArtifactId).getValue());

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag plugin = (Xml.Tag) super.visitTag(tag, ctx);
                if (isPluginTag("org.apache.maven.plugins", "maven-compiler-plugin")) {
                    plugin = maybeUpdatePlugin(plugin, ctx);
                    if (plugin != tag) {
                        maybeUpdateModel();
                    }
                }
                return plugin;
            }

            private Xml.Tag maybeUpdatePlugin(Xml.Tag plugin, ExecutionContext ctx) {
                return mapTagChildren(plugin, childTag -> "configuration".equals(childTag.getName()) ? maybeUpdateConfiguration(childTag, ctx) : childTag);
            }

            private Xml.Tag maybeUpdateConfiguration(Xml.Tag configuration, ExecutionContext ctx) {
                return mapTagChildren(configuration, childTag -> "annotationProcessorPaths".equals(childTag.getName()) ? maybeUpdateAnnotationProcessorPaths(childTag, ctx) : childTag);
            }

            private Xml.Tag maybeUpdateAnnotationProcessorPaths(Xml.Tag annotationProcessorPaths, ExecutionContext ctx) {
                return mapTagChildren(annotationProcessorPaths, childTag -> {
                    if ("path".equals(childTag.getName()) && isPathMatch(childTag)) {
                        Xml.Tag path = childTag;
                        if (newGroupId != null && !newGroupId.equals(path.getChildValue("groupId").orElse(""))) {
                            path = changeChildTagValue(path, "groupId", newGroupId, ctx);
                        }
                        if (newArtifactId != null && !newArtifactId.equals(path.getChildValue("artifactId").orElse(""))) {
                            path = changeChildTagValue(path, "artifactId", newArtifactId, ctx);
                        }
                        if (newVersion != null) {
                            String versionToUpdate = newVersion.startsWith("micronaut.") ? "${" + newVersion + "}" : newVersion;
                            if (!versionToUpdate.equals(path.getChildValue("version").orElse(""))) {
                                path = changeChildTagValue(path, "version",
                                        versionToUpdate,
                                        ctx);
                            }
                        }
                        if (exclusions == null) {
                            path = filterTagChildren(path, child -> !"exclusions".equals(child.getName()));
                        } else {
                            maybeAddExclusionsToPath(path, exclusions.stream().filter(s -> !StringUtils.isBlank(s)).collect(Collectors.toList()));
                        }
                        childTag = path;
                    }
                    return childTag;
                });
            }

            private void maybeAddExclusionsToPath(@NonNull Xml.Tag path, @NonNull List<String> exclusionsToAdd) {
                if (!exclusionsToAdd.isEmpty()) {
                    Xml.Tag exclusionsTag = Xml.Tag.build("\n<exclusions>\n" +
                            MavenExclusions.buildContent(exclusionsToAdd) +
                            "</exclusions>");
                    doAfterVisit(new AddOrUpdateChild<>(path, exclusionsTag));
                }
            }

            private boolean isPathMatch(Xml.Tag path) {
                return this.depMatcher.matches(path.getChildValue("groupId").orElse(""),
                        path.getChildValue("artifactId").orElse(""));
            }

            private Xml.Tag changeChildTagValue(Xml.Tag tag, String childTagName, String newValue, ExecutionContext ctx) {
                Optional<Xml.Tag> childTag = tag.getChild(childTagName);
                if (childTag.isPresent() && !newValue.equals(childTag.get().getValue().orElse(null))) {
                    tag = (Xml.Tag) new ChangeTagValueVisitor<>(childTag.get(), newValue).visitNonNull(tag, ctx);
                }
                return tag;
            }
        };
    }
}
