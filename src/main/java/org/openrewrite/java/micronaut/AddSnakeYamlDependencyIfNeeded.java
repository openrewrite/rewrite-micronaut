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
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.dependencies.AddDependency;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class AddSnakeYamlDependencyIfNeeded extends ScanningRecipe<AddSnakeYamlDependencyIfNeeded.Accumulator> {

    @Getter
    final String displayName = "Add `snakeyaml` dependency if needed";

    @Getter
    final String description = "This recipe will add the `snakeyaml` dependency to a Micronaut 4 application that uses yaml configuration.";

    @Override
    public AddSnakeYamlDependencyIfNeeded.Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(new HashSet<>(), new HashSet<>(), addDependencyRecipe().getInitialValue(ctx));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AddSnakeYamlDependencyIfNeeded.Accumulator acc) {
        AddDependency addDependencyRecipe = addDependencyRecipe();
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile source = (SourceFile) tree;
                    Path path = source.getSourcePath().toAbsolutePath().normalize();
                    String name = path.getFileName().toString();
                    if ("pom.xml".equals(name) || "build.gradle".equals(name) || "build.gradle.kts".equals(name)) {
                        acc.projectDirectories.add(path.getParent());
                    }
                    if (tree != new FindYamlConfig().getVisitor().visit(tree, ctx)) {
                        acc.yamlConfigs.add(path);
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
        // Prefer the closest build file so a child module does not enable the root project.
        Set<Path> projectsUsingYaml = new HashSet<>();
        for (Path yaml : acc.yamlConfigs) {
            Path owner = null;
            for (Path project : acc.projectDirectories) {
                if (yaml.startsWith(project) && (owner == null || project.getNameCount() > owner.getNameCount())) {
                    owner = project;
                }
            }
            if (owner != null) {
                projectsUsingYaml.add(owner);
            }
        }
        return Preconditions.check(new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile && projectsUsingYaml.contains(
                        ((SourceFile) tree).getSourcePath().toAbsolutePath().normalize().getParent())) {
                    return SearchResult.found(tree);
                }
                return tree;
            }
        }, addDependencyRecipe().getVisitor(acc.getAddDependencyAccumulator()));
    }

    private static AddDependency addDependencyRecipe() {
        return new AddDependency(
                "org.yaml", "snakeyaml", null, null, null,
                null, null, null, "runtimeOnly", "runtime", null, null, null, null);
    }

    @AllArgsConstructor
    @Data
    public static class Accumulator {
        Set<Path> projectDirectories;
        Set<Path> yamlConfigs;
        AddDependency.Accumulator addDependencyAccumulator;
    }
}
