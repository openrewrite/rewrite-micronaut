package org.openrewrite.java.micronaut;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.AddOrUpdateChild;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;
import static org.openrewrite.xml.MapTagChildrenVisitor.mapTagChildren;

@Value
@EqualsAndHashCode(callSuper = true)
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
            description = "An exact version string for the annotation processor path.",
            example = "${micronaut.validation}",
            required = false)
    @Nullable
    String newVersion;

    @Option(displayName = "Exclusions",
            description = "A list of exclustions to apply to the annotation processor path in the format groupId:artifactId",
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
                       if (newGroupId != null) {
                           path = changeChildTagValue(path, "groupId", newGroupId, ctx);
                       }
                       if (newArtifactId != null) {
                           path = changeChildTagValue(path, "artifactId", newArtifactId, ctx);
                       }
                       if (newVersion != null) {
                           path = changeChildTagValue(path, "version", newVersion, ctx);
                       }
                       if (exclusions == null) {
                           path = filterTagChildren(path, child -> !("exclusions".equals(child.getName())));
                       } else if (exclusions != null) {
                           path = addExclusionsToPath(path, ctx);
                       }
                       childTag = path;
                   }
                   return childTag;
                });
            }

            private Xml.Tag addExclusionsToPath(Xml.Tag path, ExecutionContext ctx) {
                Xml.Tag exclusionsTag = Xml.Tag.build("\n<exclusions>\n" + buildExclusionsContent() + "</exclusions>");
                doAfterVisit(new AddOrUpdateChild<>(path, exclusionsTag));
                return path;
            }

            private String buildExclusionsContent() {
                if (exclusions == null) {
                    return "";
                }
                return exclusions.stream().map(exclusion -> {
                    StringBuilder exclusionContent = new StringBuilder("<exclusion>\n");
                    String[] exclusionParts = exclusion.split(":");
                    if (exclusionParts.length != 2) {
                        throw new IllegalStateException("Expected an exclusion in the form of groupId:artifactId but was '" + exclusion + "'");
                    }
                    exclusionContent.append("<groupId>").append(exclusionParts[0]).append("</groupId>\n")
                            .append("<artifactId>").append(exclusionParts[1]).append("</artifactId>\n")
                            .append("</exclusion>\n");
                    return exclusionContent.toString();
                }).collect(Collectors.joining());
            }

            private boolean isPathMatch(Xml.Tag path) {
                return oldGroupId.equals(path.getChildValue("groupId").orElse(null)) &&
                        oldArtifactId.equals(path.getChildValue("artifactId").orElse(null));
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
