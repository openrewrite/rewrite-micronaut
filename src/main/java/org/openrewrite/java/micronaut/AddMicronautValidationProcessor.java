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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.AddDependency;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaSourceFile;

import java.util.LinkedHashMap;
import java.util.Map;

public class AddMicronautValidationProcessor extends ScanningRecipe<Map<String, AddDependency.Scanned>> {
    @Getter
    final String displayName = "Add the Micronaut validation processor to Gradle source sets";

    @Getter
    final String description = "Add the validation annotation processor to each source set that uses validation constraints.";

    @Override
    public Map<String, AddDependency.Scanned> getInitialValue(ExecutionContext ctx) {
        return new LinkedHashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<String, AddDependency.Scanned> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile && isSource((JavaSourceFile) tree)) {
                    JavaSourceFile source = (JavaSourceFile) tree;
                    String sourceSet = source.getMarkers().findFirst(JavaSourceSet.class)
                            .map(JavaSourceSet::getName).orElse("main");
                    String configuration = processorConfiguration(source, sourceSet);
                    AddDependency recipe = dependency(configuration);
                    recipe.getScanner(acc.computeIfAbsent(configuration, k -> recipe.getInitialValue(ctx))).visit(tree, ctx);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<String, AddDependency.Scanned> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree result = tree;
                for (Map.Entry<String, AddDependency.Scanned> entry : acc.entrySet()) {
                    result = dependency(entry.getKey()).getVisitor(entry.getValue()).visit(result, ctx);
                }
                return result;
            }
        };
    }

    private static String processorConfiguration(JavaSourceFile source, String sourceSet) {
        String path = source.getSourcePath().toString();
        if (path.endsWith(".kt")) {
            return "main".equals(sourceSet) ? "kapt" :
                    "kapt" + Character.toUpperCase(sourceSet.charAt(0)) + sourceSet.substring(1);
        }
        String configuration = path.endsWith(".groovy") ? "compileOnly" : "annotationProcessor";
        return "main".equals(sourceSet) ? configuration :
                sourceSet + Character.toUpperCase(configuration.charAt(0)) + configuration.substring(1);
    }

    private static boolean isSource(JavaSourceFile source) {
        String path = source.getSourcePath().toString();
        return path.endsWith(".java") || path.endsWith(".kt") || path.endsWith(".groovy");
    }

    private static AddDependency dependency(String configuration) {
        return new AddDependency("io.micronaut.validation", "micronaut-validation-processor", null, null,
                configuration, "jakarta.validation.constraints.*", null, null, null, null);
    }
}
