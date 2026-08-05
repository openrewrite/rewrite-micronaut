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
import org.openrewrite.*;
import org.openrewrite.yaml.ChangePropertyKey;
import org.openrewrite.yaml.UnfoldProperties;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class UpdateSecurityYamlIfNeeded extends Recipe {

    private static final String FILE_MATCHER = "**/{application,application-*,bootstrap,bootstrap-*}.{yml,yaml}";

    private static final List<String> RELOCATED_KEY_PATHS = SecurityKeyRelocations.KEY_MAPPINGS.stream()
            .map(mapping -> "$." + mapping[1])
            .collect(toList());

    @Getter
    final String displayName = "Update relocated Micronaut Security config yaml keys";

    @Getter
    final String description = "This recipe will update relocated security config keys in Micronaut configuration yaml files.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles(FILE_MATCHER).getVisitor(), new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                Yaml.Documents docs = documents;
                for (String[] mapping : SecurityKeyRelocations.KEY_MAPPINGS) {
                    docs = (Yaml.Documents) new ChangePropertyKey(mapping[0], mapping[1], null, null, null)
                            .getVisitor().visitNonNull(docs, ctx);
                    // Unfold eagerly so that the next relocated key nests into the mapping created here
                    docs = (Yaml.Documents) new UnfoldProperties(null, RELOCATED_KEY_PATHS)
                            .getVisitor().visitNonNull(docs, ctx);
                }
                return docs;
            }
        });
    }
}
