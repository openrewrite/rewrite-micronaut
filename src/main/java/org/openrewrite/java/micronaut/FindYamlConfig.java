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

import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;

public class FindYamlConfig extends Recipe {

    @Override
    public String getDisplayName() {
        return "Find Micronaut yaml config";
    }

    @Override
    public String getDescription() {
        return "Find Micronaut yaml configuration files.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindSourceFiles("**/{application,application-*,bootstrap,bootstrap-*}.{yml,yaml}").getVisitor();
    }
}
