package org.openrewrite.java.micronaut;

import org.openrewrite.*;
import org.openrewrite.gradle.AddDependency;
import org.openrewrite.internal.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class AddSnakeYamlDependencyIfNeeded extends ScanningRecipe<AddSnakeYamlDependencyIfNeeded.YamlAccumulator> {

    private final List<Recipe> recipeList = new ArrayList<>();

    static class YamlAccumulator {
        boolean usingYamlConfig = false;
    }

    public AddSnakeYamlDependencyIfNeeded() {
        recipeList.add(new AddDependency("org.yaml", "snakeyaml", null, null, "runtimeOnly", "io.micronaut.runtime.Micronaut",
                null, null, null, null));
        recipeList.add(new org.openrewrite.maven.AddDependency("org.yaml", "snakeyaml", "LATEST", null, "runtime", null, "io.micronaut.runtime.Micronaut",
                null, null, null, null, null));
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
        if (!acc.usingYamlConfig && !recipeList.isEmpty()) {
            recipeList.clear();
        }
        return super.getVisitor(acc);
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
