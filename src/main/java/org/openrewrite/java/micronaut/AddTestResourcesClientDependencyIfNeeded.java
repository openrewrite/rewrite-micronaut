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


import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.search.FindProperties;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

public class AddTestResourcesClientDependencyIfNeeded extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add `micronaut-test-resources-client` if needed";
    }

    @Override
    public String getDescription() {
        return "Add the `micronaut-test-resources-client` dependency to pom.xml if `test.resources.client.enabled property=true`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MavenIsoVisitor<ExecutionContext> resourcesEnabled = new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document maven = super.visitDocument(document, ctx);
                Optional<Xml.Tag> testResourcesProp = FindProperties.find(document, "micronaut\\.test\\.resources\\.enabled").stream().findFirst();
                if ("true".equals(testResourcesProp.flatMap(Xml.Tag::getValue).orElse("false"))) {
                    return SearchResult.found(maven);
                }
                return maven;
            }
        };
        AddDependency addDependency = new AddDependency("io.micronaut.testresources", "micronaut-test-resources-client", "LATEST",
                null, "provided", null, null, null, null, null, null, null);
        return Preconditions.check(resourcesEnabled, addDependency.getVisitor());
    }
}
