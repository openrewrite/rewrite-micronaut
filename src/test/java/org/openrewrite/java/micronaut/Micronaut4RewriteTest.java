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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.properties.Assertions.properties;

public abstract class Micronaut4RewriteTest implements RewriteTest {
    protected static String latestApplicationPluginVersion;
    protected static String latestMicronautVersion;
    private SourceSpecs gradleProperties;

    @BeforeAll
    static void init() throws MavenDownloadingException {
        latestApplicationPluginVersion = MicronautVersionHelper.getLatestMN4PluginVersion("io.micronaut.application");
        latestMicronautVersion = MicronautVersionHelper.getLatestMN4Version();
    }

    @BeforeEach
    void initGradleProperties() {
       gradleProperties = properties("micronautVersion=" + latestMicronautVersion, s -> s.path("gradle.properties"));
    }

    protected SourceSpecs getGradleProperties() {
        return gradleProperties;
    }
}
