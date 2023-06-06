package org.openrewrite.java.micronaut;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.properties.ChangePropertyKey;

import java.util.ArrayList;
import java.util.List;

public class UpdateSecurityPropertiesIfNeeded extends Recipe {

    private final List<Recipe> recipeList = new ArrayList<>();

    @Override
    public String getDisplayName() {
        return "Update relocated Micronaut Security config properties";
    }

    @Override
    public String getDescription() {
        return "This recipe will update relocated security config keys in Micronaut configuration property files.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return this.recipeList;
    }

    public UpdateSecurityPropertiesIfNeeded() {
        this.recipeList.add(new ChangePropertyKey("micronaut.security.token.jwt.generator.access-token.expiration", "micronaut.security.token.generator.access-token.expiration", null, null));
        this.recipeList.add(new ChangePropertyKey("micronaut.security.token.jwt.cookie.enabled", "micronaut.security.token.cookie.enabled", null, null));
        this.recipeList.add(new ChangePropertyKey("micronaut.security.token.jwt.cookie.cookie-max-age", "micronaut.security.token.cookie.cookie-max-age", null, null));
        this.recipeList.add(new ChangePropertyKey("micronaut.security.token.jwt.cookie.cookie-path", "micronaut.security.token.cookie.cookie-path", null, null));
        this.recipeList.add(new ChangePropertyKey("micronaut.security.token.jwt.cookie.cookie-domain", "micronaut.security.token.cookie.cookie-domain", null, null));
        this.recipeList.add(new ChangePropertyKey("micronaut.security.token.jwt.cookie.cookie-same-site", "micronaut.security.token.cookie.cookie-same-site", null, null));
        this.recipeList.add(new ChangePropertyKey("micronaut.security.token.jwt.bearer.enabled", "micronaut.security.token.bearer.enabled", null, null));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.not(new FindPropertiesConfig().getVisitor()), new TreeVisitor<Tree, ExecutionContext>() {
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
