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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.file.Path;

import static org.openrewrite.properties.Assertions.properties;

@SuppressWarnings("UnusedProperty")
class UpgradeMicronautGradlePropertiesVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeMicronautGradlePropertiesVersion("~2.1"));
    }

    @Test
    void changeValue(@TempDir Path tempDir) throws IOException {
        rewriteRun(
          properties(
            "micronautVersion=2.0.3",
            "micronautVersion=2.1.4",
            source -> source.path("gradle.properties")
          )
        );
    }

    @Test
    void upgradeToMicronaut4() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeMicronautGradlePropertiesVersion("4.0.0-M4")),
          properties(
            "micronautVersion=3.9.0",
            "micronautVersion=4.0.0-M4",
            s -> s.path("gradle.properties")
          )
        );
    }
}
