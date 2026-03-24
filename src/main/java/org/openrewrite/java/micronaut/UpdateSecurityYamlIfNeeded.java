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
import org.openrewrite.Recipe;
import org.openrewrite.yaml.CopyValue;
import org.openrewrite.yaml.DeleteKey;

import java.util.ArrayList;
import java.util.List;

public class UpdateSecurityYamlIfNeeded extends Recipe {

    private static final String FILE_MATCHER = "**/{application,application-*,bootstrap,bootstrap-*}.{yml,yaml}";

    @Getter
    private final List<Recipe> recipeList = new ArrayList<>();

    private static final String TOKEN_PATH = "$.micronaut.security.token";

    @Getter
    final String displayName = "Update relocated Micronaut Security config yaml keys";

    @Getter
    final String description = "This recipe will update relocated security config keys in Micronaut configuration yaml files.";

    public UpdateSecurityYamlIfNeeded() {
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.generator.access-token.expiration", FILE_MATCHER, TOKEN_PATH + ".generator.access-token.expiration", FILE_MATCHER, Boolean.TRUE));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.cookie.enabled", FILE_MATCHER, TOKEN_PATH + ".cookie.enabled", FILE_MATCHER, Boolean.TRUE));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.cookie.cookie-max-age", FILE_MATCHER, TOKEN_PATH + ".cookie.cookie-max-age", FILE_MATCHER, Boolean.TRUE));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.cookie.cookie-path", FILE_MATCHER, TOKEN_PATH + ".cookie.cookie-path", FILE_MATCHER, Boolean.TRUE));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.cookie.cookie-domain", FILE_MATCHER, TOKEN_PATH + ".cookie.cookie-domain", FILE_MATCHER, Boolean.TRUE));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.cookie.cookie-same-site", FILE_MATCHER, TOKEN_PATH + ".cookie.cookie-same-site", FILE_MATCHER, Boolean.TRUE));
        this.recipeList.add(new CopyValue(TOKEN_PATH + ".jwt.bearer.enabled", FILE_MATCHER, TOKEN_PATH + ".bearer.enabled", FILE_MATCHER, Boolean.TRUE));
        this.recipeList.add(new DeleteKey(TOKEN_PATH + ".jwt.generator", FILE_MATCHER));
        this.recipeList.add(new DeleteKey(TOKEN_PATH + ".jwt.cookie", FILE_MATCHER));
        this.recipeList.add(new DeleteKey(TOKEN_PATH + ".jwt.bearer", FILE_MATCHER));
        this.recipeList.add(new RemoveUnusedInConfigFiles());
    }
}
