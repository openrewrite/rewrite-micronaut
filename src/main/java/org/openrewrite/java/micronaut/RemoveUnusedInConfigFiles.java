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
import org.openrewrite.yaml.cleanup.RemoveUnused;

public class RemoveUnusedInConfigFiles extends Recipe {

    private static final String FILE_MATCHER = "**/{application,application-*,bootstrap,bootstrap-*}.{yml,yaml}";

    @Getter
    final String displayName = "Remove unused YAML keys in config files";

    @Getter
    final String description = "Remove empty YAML keys left behind after relocating security config keys.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles(FILE_MATCHER).getVisitor(), new RemoveUnused().getVisitor());
    }
}
