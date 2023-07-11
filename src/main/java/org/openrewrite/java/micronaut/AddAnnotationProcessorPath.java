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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddAnnotationProcessorPath extends ScanningRecipe<AddAnnotationProcessorPath.Scanned> {

    @Option(displayName = "GroupId",
            description = "The groupId to use.",
            example = "corp.internal.openrewrite.recipe")
    String groupId;

    @Option(displayName = "ArtifactId",
            description = "The artifactId to use.",
            example = "my-new-annotation-processor")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version string for the annotation processor path.",
            example = "${micronaut.validation}")
    String version;

    @Option(displayName = "Only if using",
            description = "Used to determine if the annotation processor will be added.",
            example = "jakarta.validation.constraints.*")
    String onlyIfUsing;

    @Option(displayName = "Exclusions",
            description = "A list of exclusions to apply to the annotation processor path in the format groupId:artifactId",
            example = "io.micronaut:micronaut-inject",
            required = false)
    @Nullable
    List<String> exclusions;

    @Override
    public String getDisplayName() {
        return "Add Maven annotation processor path";
    }

    @Override
    public String getDescription() {
        return "Add the groupId, artifactId, version, and exclusions of a Maven annotation processor path.";
    }

    public static class Scanned {
        boolean usingType;
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        return new Scanned();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (tree instanceof JavaSourceFile) {
                    boolean sourceFileUsesType = sourceFile != new UsesType<>(onlyIfUsing, true).visit(sourceFile, ctx);
                    acc.usingType |= sourceFileUsesType;
                }
                return sourceFile;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(acc.usingType, new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                return new CheckAnnotationProcessorPathVisitor().visitNonNull(document, ctx);
            }
        });
    }

    private class CheckAnnotationProcessorPathVisitor extends MavenIsoVisitor<ExecutionContext> {

        private final XPathMatcher ANNOTATION_PROCESSOR_PATH_MATCHER = new XPathMatcher("/project/build/plugins/plugin/configuration/annotationProcessorPaths/path");

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (ANNOTATION_PROCESSOR_PATH_MATCHER.matches(getCursor()) &&
                    groupId.equals(tag.getChildValue("groupId").orElse(null)) &&
                    artifactId.equals(tag.getChildValue("artifactId").orElse(null))) {
                getCursor().putMessageOnFirstEnclosing(Xml.Document.class, "alreadyHasAnnotationProcessor", true);
                return tag;
            }
            return super.visitTag(tag, ctx);
        }

        @Override
        public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
            Xml.Document maven = super.visitDocument(document, ctx);

            if (getCursor().getMessage("alreadyHasAnnotationProcessor", false)) {
                return document;
            }

            doAfterVisit(new InsertAnnotationProcessorPath());

            return maven;
        }
    }

    private class InsertAnnotationProcessorPath extends MavenIsoVisitor<ExecutionContext> {

        private final XPathMatcher ANNOTATION_PROCESSOR_PATHS_MATCHER = new XPathMatcher("/project/build/plugins/plugin/configuration/annotationProcessorPaths");

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (ANNOTATION_PROCESSOR_PATHS_MATCHER.matches(getCursor())) {
                Xml.Tag pathTag = Xml.Tag.build(
                        "\n<path>\n" +
                                "<groupId>" + groupId + "</groupId>\n" +
                                "<artifactId>" + artifactId + "</artifactId>\n" +
                                "<version>" + version + "</version>\n" +
                                buildExclusionsContent() +
                                "</path>"
                );

                doAfterVisit(new AddToTagVisitor<>(tag, pathTag));

                maybeUpdateModel();

                return tag;
            }
            return super.visitTag(tag, ctx);
        }

        private String buildExclusionsContent() {
            if (exclusions == null) {
                return "";
            }
            return "<exclusions>\n" +
                    exclusions.stream().map(exclusion -> {
                        StringBuilder exclusionContent = new StringBuilder("<exclusion>\n");
                        String[] exclusionParts = exclusion.split(":");
                        if (exclusionParts.length != 2) {
                            throw new IllegalStateException("Expected an exclusion in the form of groupId:artifactId but was '" + exclusion + "'");
                        }
                        exclusionContent.append("<groupId>").append(exclusionParts[0]).append("</groupId>\n")
                                .append("<artifactId>").append(exclusionParts[1]).append("</artifactId>\n")
                                .append("</exclusion>\n");
                        return exclusionContent.toString();
                    }).collect(Collectors.joining()) +
                    "</exclusions>";
        }
    }
}
