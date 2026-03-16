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
import org.openrewrite.*;
import org.openrewrite.properties.ChangePropertyKey;

import java.util.Arrays;
import java.util.List;

public class UpdateSecurityPropertiesIfNeeded extends Recipe {

    private static final String FILE_MATCHER = "**/{application,application-*,bootstrap,bootstrap-*}.properties";

    private static final List<String[]> KEY_MAPPINGS = Arrays.asList(
            new String[]{"micronaut.security.token.jwt.generator.access-token.expiration", "micronaut.security.token.generator.access-token.expiration"},
            new String[]{"micronaut.security.token.jwt.cookie.enabled", "micronaut.security.token.cookie.enabled"},
            new String[]{"micronaut.security.token.jwt.cookie.cookie-max-age", "micronaut.security.token.cookie.cookie-max-age"},
            new String[]{"micronaut.security.token.jwt.cookie.cookie-path", "micronaut.security.token.cookie.cookie-path"},
            new String[]{"micronaut.security.token.jwt.cookie.cookie-domain", "micronaut.security.token.cookie.cookie-domain"},
            new String[]{"micronaut.security.token.jwt.cookie.cookie-same-site", "micronaut.security.token.cookie.cookie-same-site"},
            new String[]{"micronaut.security.token.jwt.bearer.enabled", "micronaut.security.token.bearer.enabled"}
    );

    @Getter
    final String displayName = "Update relocated Micronaut Security config properties";

    @Getter
    final String description = "This recipe will update relocated security config keys in Micronaut configuration property files.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles(FILE_MATCHER).getVisitor(), new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                for (String[] mapping : KEY_MAPPINGS) {
                    tree = new ChangePropertyKey(mapping[0], mapping[1], null, null).getVisitor().visit(tree, ctx);
                }
                return tree;
            }
        });
    }
}
