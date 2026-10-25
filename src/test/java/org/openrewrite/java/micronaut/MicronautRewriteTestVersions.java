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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenSettings;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.MavenRepositoryCredentials;
import org.openrewrite.maven.tree.MavenRepositoryMirror;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.openrewrite.internal.StringUtils.isNullOrEmpty;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_CENTRAL;
import static org.openrewrite.maven.tree.MavenRepository.MAVEN_LOCAL_DEFAULT;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MicronautRewriteTestVersions {
    private static final String MIRROR_ID = "maven-central-mirror";

    /**
     * The Gradle plugin portal, by the id {@link MicronautVersionHelper} gives it.
     */
    private static final MavenRepository GRADLE_PLUGIN_PORTAL = MavenRepository.builder()
      .id("gradle-plugins")
      .uri("https://plugins.gradle.org/m2/")
      .build();

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

    private static String resolveVersion(String versionPattern, String currentVersion) {
        try {
            return MicronautVersionHelper.getNewerVersion(versionPattern, currentVersion, withMavenCentralMirror(new InMemoryExecutionContext())).orElse(currentVersion);
        } catch (MavenDownloadingException e) {
            throw new IllegalStateException("Failed to resolve latest Micronaut Framework %s version".formatted(versionPattern), e);
        }
    }

    private static String resolvePluginVersion(String pluginId) {
        try {
            return MicronautVersionHelper.getNewerGradlePluginVersion(pluginId, "4.x", "4.0.0", withMavenCentralMirror(new InMemoryExecutionContext())).orElse("4.0.0");
        } catch (MavenDownloadingException e) {
            throw new IllegalStateException("Failed to resolve plugin version for " + pluginId, e);
        }
    }

    /**
     * Loads ~/.m2/settings.xml into {@code ctx} as {@code pomXml(..)} does, then adds the mirror that
     * MAVEN_CENTRAL_MIRROR_URL, _USERNAME and _PASSWORD name, if they are set, for whichever of Maven Central and the
     * Gradle plugin portal no mirror serves yet. For the contexts the lookups above make, and for tests that parse no
     * POM, which never load the settings.
     */
    public static <T extends ExecutionContext> T withMavenCentralMirror(T ctx) {
        MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
        boolean nothingConfigured = mctx.getSettings() == null &&
          mctx.getLocalRepository().equals(MAVEN_LOCAL_DEFAULT) &&
          mctx.getRepositories().isEmpty() &&
          mctx.getActiveProfiles().isEmpty() &&
          mctx.getMirrors().isEmpty();
        if (nothingConfigured) {
            mctx.setMavenSettings(MavenSettings.readMavenSettingsFromDisk(mctx));
        }
        String url = System.getenv("MAVEN_CENTRAL_MIRROR_URL");
        String username = System.getenv("MAVEN_CENTRAL_MIRROR_USERNAME");
        String password = System.getenv("MAVEN_CENTRAL_MIRROR_PASSWORD");
        MavenSettings settings = mctx.getSettings();
        Collection<MavenRepositoryMirror> mirrors = mctx.getMirrors(settings);
        String unmirrored = Stream.of(MAVEN_CENTRAL, GRADLE_PLUGIN_PORTAL)
          .filter(repository -> mirrors.stream().noneMatch(mirror -> mirror.matches(repository)))
          .map(MavenRepository::getId)
          .collect(joining(","));
        if (isNullOrEmpty(url) || isNullOrEmpty(username) || isNullOrEmpty(password) || unmirrored.isEmpty()) {
            return ctx;
        }
        if (settings != null) {
            // Where there are settings, the downloader takes its mirrors from them
            mctx.setMavenSettings(settings.merge(new MavenSettings(null, null, null,
              new MavenSettings.Mirrors(singletonList(new MavenSettings.Mirror(MIRROR_ID, url, unmirrored, null, null))),
              new MavenSettings.Servers(singletonList(new MavenSettings.Server(MIRROR_ID, username, password, null))))));
        } else {
            List<MavenRepositoryMirror> withMirror = new ArrayList<>(mirrors);
            withMirror.add(new MavenRepositoryMirror(MIRROR_ID, url, unmirrored, null, null, null));
            mctx.setMirrors(withMirror);
            List<MavenRepositoryCredentials> credentials = new ArrayList<>(mctx.getCredentials());
            credentials.add(new MavenRepositoryCredentials(MIRROR_ID, username, password));
            mctx.setCredentials(credentials);
        }
        return ctx;
    }
}
