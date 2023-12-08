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

    private static final String TOKEN_PATH = "$.micronaut.security.token";

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
        return Preconditions.check(Preconditions.not(new FindYamlConfig().getVisitor()), new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!recipeList.isEmpty()) {
                    recipeList.clear();
                }
                return super.visit(tree, ctx);
            }
        });
    }
}
