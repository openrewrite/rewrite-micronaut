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

import org.jetbrains.annotations.NotNull;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenDownloadingException;

public class MicronautRewriteTestVersions {
    private static final String MN_2_VERSION;
    private static final String MN_3_VERSION;
    private static final String MN_4_VERSION;

    private static final String MN_4_APPLICATION_PLUGIN_VERSION;

    static {
        MN_2_VERSION = resolveVersion("2.x", "2.0.0");
        MN_3_VERSION = resolveVersion("3.x", "3.0.0");
        MN_4_VERSION = resolveVersion("4.x", "4.0.0");
        MN_4_APPLICATION_PLUGIN_VERSION = resolvePluginVersion("io.micronaut.application");
    }

    public static String getLatestMN2Version() {
        return MN_2_VERSION;
    }

    public static String getLatestMN3Version() {
        return MN_3_VERSION;
    }

    public static String getLatestMN4Version() {
        return MN_4_VERSION;
    }

    public static String getLatestMN4ApplicationPluginVersion() {
        return MN_4_APPLICATION_PLUGIN_VERSION;
    }

    public static String getLatestMN4PluginVersion(String pluginId) {
        return resolvePluginVersion(pluginId);
    }

    @NotNull
    private static String resolveVersion(String versionPattern, String currentVersion) {
        try {
            return MicronautVersionHelper.getNewerVersion(versionPattern, currentVersion, new InMemoryExecutionContext()).orElse(currentVersion);
        } catch (MavenDownloadingException e) {
            throw new IllegalStateException("Failed to resolve latest Micronaut Framework %s version".formatted(versionPattern), e);
        }
    }

    @NotNull
    private static String resolvePluginVersion(String pluginId) {
        try {
            return MicronautVersionHelper.getNewerGradlePluginVersion(pluginId, "4.x", "4.0.0", new InMemoryExecutionContext()).orElse("4.0.0");
        } catch (MavenDownloadingException e) {
            throw new IllegalStateException("Failed to resolve plugin version for " + pluginId, e);
        }
    }

    private MicronautRewriteTestVersions() {
    }
}
