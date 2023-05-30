package org.openrewrite.java.micronaut;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.yaml.CopyValue;
import org.openrewrite.yaml.DeleteKey;
import org.openrewrite.yaml.MergeYaml;
import org.openrewrite.yaml.cleanup.RemoveUnused;

import java.util.ArrayList;
import java.util.List;

public class UpdateSecurityYamlIfNeeded extends Recipe {

    private final List<Recipe> recipeList = new ArrayList<>();
    
    private final String newYamlKeysSnippet =
            "generator:\n" +
            "  access-token:\n" +
            "    expiration:\n" +
            "cookie:\n" +
            "  enabled:\n" +
            "  cookie-max-age:\n" +
            "  cookie-path:\n" +
            "  cookie-domain:\n" +
            "  cookie-same-site:\n" +
            "bearer:\n" +
            "  enabled:";

    private final String TOKEN_PATH = "$.micronaut.security.token";

    @Override
    public String getDisplayName() {
        return "Update relocated Micronaut Security config yaml keys";
    }

    @Override
    public String getDescription() {
        return "This recipe will update relocated security config keys in Micronaut configuration yaml files.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return this.recipeList;
    }

    public UpdateSecurityYamlIfNeeded() {
        this.recipeList.add(new MergeYaml("$.micronaut.security.token", newYamlKeysSnippet, Boolean.TRUE, null));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.generator.access-token.expiration", TOKEN_PATH + ".generator.access-token.expiration"));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.cookie.enabled", TOKEN_PATH + ".cookie.enabled"));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.cookie.cookie-max-age", TOKEN_PATH + ".cookie.cookie-max-age"));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.cookie.cookie-path", TOKEN_PATH + ".cookie.cookie-path"));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.cookie.cookie-domain", TOKEN_PATH + ".cookie.cookie-domain"));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.cookie.cookie-same-site", TOKEN_PATH + ".cookie.cookie-same-site"));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.bearer.enabled", TOKEN_PATH + ".bearer.enabled"));
        this.recipeList.add(new DeleteKey(TOKEN_PATH + ".jwt.generator"));
        this.recipeList.add(new DeleteKey(TOKEN_PATH + ".jwt.cookie"));
        this.recipeList.add(new DeleteKey(TOKEN_PATH + ".jwt.bearer"));
        this.recipeList.add(new RemoveUnused());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindYamlConfig(), super.getVisitor());
    }
}
