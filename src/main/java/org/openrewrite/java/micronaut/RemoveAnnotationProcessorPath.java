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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;
import static org.openrewrite.xml.MapTagChildrenVisitor.mapTagChildren;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveAnnotationProcessorPath extends Recipe {

    @Option(displayName = "GroupId",
            description = "The groupId to use.",
            example = "corp.internal.openrewrite.recipe")
    String groupId;

    @Option(displayName = "ArtifactId",
            description = "The artifactId to use.",
            example = "my-new-annotation-processor")
    String artifactId;

    @Override
    public String getDisplayName() {
        return "Remove Maven annotation processor path";
    }

    @Override
    public String getDescription() {
        return "Remove the Maven annotation processor path that matches the given groupId and artifactId.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag plugin = super.visitTag(tag, ctx);
                if (isPluginTag("org.apache.maven.plugins", "maven-compiler-plugin")) {
                    plugin = maybeUpdatePlugin(plugin);
                    if (plugin != tag) {
                        maybeUpdateModel();
                    }
                }
                return plugin;
            }

            private Xml.Tag maybeUpdatePlugin(Xml.Tag plugin) {
                return mapTagChildren(plugin,
                        childTag -> "configuration".equals(childTag.getName()) ? maybeUpdateConfiguration(childTag) : childTag);
            }

            private Xml.Tag maybeUpdateConfiguration(Xml.Tag configuration) {
                return mapTagChildren(configuration,
                        childTag -> "annotationProcessorPaths".equals(childTag.getName()) ? maybeUpdateAnnotationProcessorPaths(childTag) : childTag);
            }

            private Xml.Tag maybeUpdateAnnotationProcessorPaths(Xml.Tag annotationProcessorPaths) {
                return filterTagChildren(annotationProcessorPaths,
                        childTag -> !("path".equals(childTag.getName()) && isPathMatch(childTag)));
            }

            private boolean isPathMatch(Xml.Tag path) {
                return groupId.equals(path.getChildValue("groupId").orElse(null)) &&
                       artifactId.equals(path.getChildValue("artifactId").orElse(null));
            }
        };
    }
}
