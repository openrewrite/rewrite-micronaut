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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.dependencies.AddDependency;

public class AddSnakeYamlDependencyIfNeeded extends ScanningRecipe<AddSnakeYamlDependencyIfNeeded.Accumulator> {

    @Override
    public String getDisplayName() {
        return "Add `snakeyaml` dependency if needed";
    }

    @Override
    public String getDescription() {
        return "This recipe will add the `snakeyaml` dependency to a Micronaut 4 application that uses yaml configuration.";
    }

    @Override
    public AddSnakeYamlDependencyIfNeeded.Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(false, addDependencyRecipe().getInitialValue(ctx));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AddSnakeYamlDependencyIfNeeded.Accumulator acc) {
        AddDependency addDependencyRecipe = addDependencyRecipe();
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    if (!acc.usesYamlConfig) {
                        acc.usesYamlConfig = (tree != new FindYamlConfig().getVisitor().visit(tree, ctx));
                    }
                    TreeVisitor<?, ExecutionContext> addDependencyScanner = addDependencyRecipe.getScanner(acc.getAddDependencyAccumulator());
                    if (addDependencyScanner.isAcceptable((SourceFile) tree, ctx)) {
                        addDependencyScanner.visit(tree, ctx);
                    }
                }
                return super.visit(tree, ctx);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AddSnakeYamlDependencyIfNeeded.Accumulator acc) {
        return Preconditions.check(acc.usesYamlConfig, addDependencyRecipe().getVisitor(acc.getAddDependencyAccumulator()));
    }

    private static AddDependency addDependencyRecipe() {
        return new AddDependency(
                "org.yaml", "snakeyaml", null, null, null,
                null, null, null, "runtimeOnly", "runtime", null, null, null, null);
    }

    @AllArgsConstructor
    @Data
    public static class Accumulator {
        boolean usesYamlConfig;
        AddDependency.Accumulator addDependencyAccumulator;
    }
}
