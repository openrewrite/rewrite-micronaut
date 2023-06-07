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
import org.openrewrite.java.dependencies.AddDependency;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class AddSnakeYamlDependencyIfNeeded extends ScanningRecipe<AddSnakeYamlDependencyIfNeeded.YamlAccumulator> {

    private final List<Recipe> recipeList = new ArrayList<>();

    static class YamlAccumulator {
        boolean usingYamlConfig = false;
    }

    public AddSnakeYamlDependencyIfNeeded() {
        recipeList.add(new AddDependency("org.yaml", "snakeyaml", null, null, "io.micronaut.runtime.Micronaut",
                null, null, null, "runtimeOnly", "runtime", null, null, null, null));
    }

    @Override
    public YamlAccumulator getInitialValue(ExecutionContext ctx) {
        return new YamlAccumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(YamlAccumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                acc.usingYamlConfig |= sourceFile != new FindYamlConfig().getVisitor().visit(sourceFile, ctx);
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(YamlAccumulator acc) {
        return Preconditions.check(!acc.usingYamlConfig, new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext executionContext) {
                if (!recipeList.isEmpty()) {
                    recipeList.clear();
                }
                return super.visit(tree, executionContext);
            }
        });
    }

    @Override
    public String getDisplayName() {
        return "Add `snakeyaml` dependency if needed";
    }

    @Override
    public String getDescription() {
        return "This recipe will add the `snakeyaml` dependency to a Micronaut 4 application that uses yaml configuration.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return this.recipeList;
    }
}
