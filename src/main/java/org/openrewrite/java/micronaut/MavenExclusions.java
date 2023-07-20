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

import org.openrewrite.internal.lang.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class MavenExclusions {
    static String buildContent(@Nullable List<String> exclusions) {
        if (exclusions == null) {
            return "";
        }

        return exclusions.stream().map(exclusion -> {
            StringBuilder exclusionContent = new StringBuilder("<exclusion>\n");
            String[] exclusionParts = exclusion.split(":");
            if (exclusionParts.length != 2) {
                throw new IllegalStateException("Expected an exclusion in the form of groupId:artifactId but was '" + exclusion + "'");
            }
            exclusionContent.append("<groupId>").append(exclusionParts[0]).append("</groupId>\n")
                    .append("<artifactId>").append(exclusionParts[1]).append("</artifactId>\n")
                    .append("</exclusion>\n");
            return exclusionContent.toString();
        }).collect(Collectors.joining());
    }
}
