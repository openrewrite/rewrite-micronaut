package org.openrewrite.java.micronaut;

import org.openrewrite.ExecutionContext;
import org.openrewrite.HasSourcePath;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

public class FindYamlConfig extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find Micronaut yaml config";
    }

    @Override
    public String getDescription() {
        return "Find Micronaut yaml configuration files.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new HasSourcePath<>("**/{application,application-*,bootstrap,bootstrap-*}.{yml,yaml}");
    }
}
