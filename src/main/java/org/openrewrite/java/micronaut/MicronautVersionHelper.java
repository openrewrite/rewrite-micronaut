/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class MicronautVersionHelper {

    private static final String GROUP_ID = "io.micronaut";
    private static final String ARTIFACT_ID = "micronaut-parent";
    private static final LatestRelease LATEST_RELEASE = new LatestRelease(null);

    public static Optional<String> getNewerVersion(String versionPattern, String currentVersion, ExecutionContext ctx) {
        VersionComparator versionComparator = Semver.validate(versionPattern, null).getValue();
        assert versionComparator != null;

        MavenMetadata mavenMetadata = new MavenPomDownloader(MavenPomCache.NOOP,
                emptyMap(), ctx).downloadMetadata(GROUP_ID, ARTIFACT_ID, emptyList());

        Collection<String> availableVersions = mavenMetadata.getVersioning().getVersions().stream()
                .filter(versionComparator::isValid)
                .collect(Collectors.toList());

        return availableVersions.stream()
                .filter(v -> LATEST_RELEASE.compare(currentVersion, v) < 0)
                .max(LATEST_RELEASE);
    }

    private MicronautVersionHelper() {
    }
}
