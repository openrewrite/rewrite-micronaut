/*
 * Copyright 2026 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * The configuration keys relocated in Micronaut Security 4, as listed under
 * <a href="https://micronaut-projects.github.io/micronaut-security/latest/guide/#_configuration_changes">Configuration changes</a>.
 * Any other key below {@code micronaut.security.token.jwt} is still valid and must be left alone.
 */
final class SecurityKeyRelocations {

    static final List<String[]> KEY_MAPPINGS = unmodifiableList(Arrays.asList(
            new String[]{"micronaut.security.token.jwt.generator.access-token.expiration", "micronaut.security.token.generator.access-token.expiration"},
            new String[]{"micronaut.security.token.jwt.cookie.enabled", "micronaut.security.token.cookie.enabled"},
            new String[]{"micronaut.security.token.jwt.cookie.cookie-max-age", "micronaut.security.token.cookie.cookie-max-age"},
            new String[]{"micronaut.security.token.jwt.cookie.cookie-path", "micronaut.security.token.cookie.cookie-path"},
            new String[]{"micronaut.security.token.jwt.cookie.cookie-domain", "micronaut.security.token.cookie.cookie-domain"},
            new String[]{"micronaut.security.token.jwt.cookie.cookie-same-site", "micronaut.security.token.cookie.cookie-same-site"},
            new String[]{"micronaut.security.token.jwt.bearer.enabled", "micronaut.security.token.bearer.enabled"}
    ));

    private SecurityKeyRelocations() {
    }
}
