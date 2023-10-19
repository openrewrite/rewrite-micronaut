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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.search.FindProperties;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddTestResourcesClientDependencyIfNeeded extends ScanningRecipe<AddTestResourcesClientDependencyIfNeeded.Scanned> {

    private final List<Recipe> recipeList = new ArrayList<>();

    static class Scanned {
        boolean isTestResourcesEnabled = false;
    }

    public AddTestResourcesClientDependencyIfNeeded() {
        recipeList.add(new AddDependency("io.micronaut.testresources", "micronaut-test-resources-client", "LATEST",
                null, "provided", null, "io.micronaut.runtime.Micronaut", null, null, null, null, null));
    }

    @Override
    public String getDisplayName() {
        return "Add Test Resources Client dependency if needed";
    }

    @Override
    public String getDescription() {
        return "This recipe adds the Test Resources Client dependency to pom.xml if test.resources.client.enabled property is true.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return this.recipeList;
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        return new Scanned();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
                Xml.Document maven = super.visitDocument(document, executionContext);
                Optional<Xml.Tag> testResourcesProp = FindProperties.find(document, "micronaut\\.test\\.resources\\.enabled").stream().findFirst();
                if ("true".equals(testResourcesProp.flatMap(Xml.Tag::getValue).orElse("false"))) {
                    acc.isTestResourcesEnabled = true;
                }
                return maven;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(!acc.isTestResourcesEnabled, new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                if (!recipeList.isEmpty()) {
                    recipeList.clear();
                }
                return super.visit(tree, executionContext);
            }
        });
    }
}
