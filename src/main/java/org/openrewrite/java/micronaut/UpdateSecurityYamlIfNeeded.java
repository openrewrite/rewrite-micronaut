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
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.ShiftFormatLeftVisitor;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.ArrayList;
import java.util.List;

public class UpdateSecurityYamlIfNeeded extends Recipe {

    private static final String FILE_MATCHER = "**/{application,application-*,bootstrap,bootstrap-*}.{yml,yaml}";

    @Getter
    final String displayName = "Update relocated Micronaut Security config yaml keys";

    @Getter
    final String description = "This recipe will update relocated security config keys in Micronaut configuration yaml files.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher jwtMatcher = new JsonPathMatcher("$.micronaut.security.token.jwt");
        return Preconditions.check(new FindSourceFiles(FILE_MATCHER).getVisitor(),
                new YamlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                        Yaml.Mapping m = super.visitMapping(mapping, ctx);
                        Yaml.Mapping.Entry jwtEntry = null;
                        for (Yaml.Mapping.Entry entry : m.getEntries()) {
                            if (entry.getValue() instanceof Yaml.Mapping &&
                                    jwtMatcher.matches(new Cursor(getCursor(), entry))) {
                                jwtEntry = entry;
                                break;
                            }
                        }
                        if (jwtEntry == null) {
                            return m;
                        }
                        Yaml.Mapping jwtMapping = (Yaml.Mapping) jwtEntry.getValue();
                        // Calculate indent shift: difference between jwt child indent and jwt indent
                        int jwtIndent = indentOf(jwtEntry.getPrefix());
                        int childIndent = jwtMapping.getEntries().isEmpty() ? jwtIndent :
                                indentOf(jwtMapping.getEntries().get(0).getPrefix());
                        int shift = childIndent - jwtIndent;

                        List<Yaml.Mapping.Entry> newEntries = new ArrayList<>();
                        for (Yaml.Mapping.Entry entry : m.getEntries()) {
                            if (entry.getKey().getValue().equals(jwtEntry.getKey().getValue())) {
                                for (Yaml.Mapping.Entry child : jwtMapping.getEntries()) {
                                    Yaml.Mapping.Entry promoted = child.withPrefix(jwtEntry.getPrefix());
                                    if (shift > 0 && promoted.getValue() instanceof Yaml.Mapping) {
                                        doAfterVisit(new ShiftFormatLeftVisitor<>(promoted.getValue(), shift));
                                    }
                                    newEntries.add(promoted);
                                }
                            } else {
                                newEntries.add(entry);
                            }
                        }
                        return m.withEntries(newEntries);
                    }

                    private int indentOf(String prefix) {
                        int lastNewline = prefix.lastIndexOf('\n');
                        return lastNewline >= 0 ? prefix.length() - lastNewline - 1 : prefix.length();
                    }
                });
    }
}
