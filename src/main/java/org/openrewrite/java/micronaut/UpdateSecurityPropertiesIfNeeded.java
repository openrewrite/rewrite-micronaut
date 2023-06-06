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
