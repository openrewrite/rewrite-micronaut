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
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.marker.GradleBuildscript;
import org.openrewrite.gradle.marker.GradlePluginDescriptor;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;

public final class MicronautVersionHelper {

    private static final String GROUP_ID = "io.micronaut";
    private static final String V4_GROUP_ID = "io.micronaut.platform";
    private static final String ARTIFACT_ID = "micronaut-parent";
    private static final LatestRelease LATEST_RELEASE = new LatestRelease(null);

    public static Optional<String> getNewerVersion(String versionPattern, String currentVersion, ExecutionContext ctx) throws MavenDownloadingException {
        VersionComparator versionComparator = Semver.validate(versionPattern, null).getValue();
        assert versionComparator != null;

        String groupId = "4".equals(Semver.majorVersion(versionPattern)) ? V4_GROUP_ID : GROUP_ID;

        MavenMetadata mavenMetadata = new MavenPomDownloader(emptyMap(), ctx)
                .downloadMetadata(new GroupArtifact(groupId, ARTIFACT_ID), null, emptyList());

        Collection<String> availableVersions = new ArrayList<>();
        for (String v : mavenMetadata.getVersioning().getVersions()) {
            if (versionComparator.isValid(null, v)) {
                availableVersions.add(v);
            }
        }

        return availableVersions.stream()
                .filter(v -> LATEST_RELEASE.compare(null, currentVersion, v) < 0)
                .max(LATEST_RELEASE);
    }

    public static Optional<String> getNewerGradlePluginVersion(String pluginId, String versionPattern, String currentVersion, ExecutionContext ctx) throws MavenDownloadingException {
        MavenRepository gradlePluginsRepo = new MavenRepository("gradle-plugins", "https://plugins.gradle.org/m2/", "true", "false", true, null, null, null, true);
        GradleSettings gradleSettings = new GradleSettings(
                randomId(),
                singletonList(gradlePluginsRepo),
                singletonList(new GradlePluginDescriptor("io.micronaut.gradle.MicronautApplicationPlugin", null)),
                emptyMap(),
                new GradleBuildscript(randomId(), singletonList(gradlePluginsRepo), emptyMap()));
        return Optional.ofNullable(new DependencyVersionSelector(null, null, gradleSettings)
                .select(new GroupArtifactVersion(pluginId, pluginId + ".gradle.plugin", currentVersion),
                        "classpath",
                        versionPattern,
                        (String) null,
                        ctx));
    }

    private MicronautVersionHelper() {
    }
}
