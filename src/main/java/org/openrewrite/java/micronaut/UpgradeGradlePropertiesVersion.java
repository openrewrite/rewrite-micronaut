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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HasSourcePath;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeGradlePropertiesVersion extends Recipe {

    private static final String PROPERTY_KEY = "micronautVersion";
    private static final String FILE_MATCHER = "**/gradle.properties";
    private static final String GROUP_ID = "io.micronaut";
    private static final String ARTIFACT_ID = "micronaut-parent";

    @Option(displayName = "New version",
            description = "An exact version number, or node-style semver selector used to select the version number.",
            example = "3.x")
    String newVersion;

    @Override
    public Validated validate() {
        Validated validated = super.validate();
        //noinspection ConstantConditions
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, null));
        }
        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Upgrade gradle.properties Micronaut version";
    }

    @Override
    public String getDescription() {
        return "Set the gradle.properties version number according to a node-style semver selector or to a specific version number.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new HasSourcePath<>(FILE_MATCHER);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangePropertyValueVisitor<>(newVersion);
    }

    private static class ChangePropertyValueVisitor<P> extends PropertiesVisitor<P> {

        @Nullable
        private Collection<String> availableVersions;

        private final VersionComparator versionComparator;
        private final ExecutionContext ctx = new InMemoryExecutionContext();

        public ChangePropertyValueVisitor(String newVersion) {
            versionComparator = Semver.validate(newVersion, null).getValue();
        }

        @Override
        public Properties visitEntry(Properties.Entry entry, P p) {
            if (entry.getKey().equals(PROPERTY_KEY)) {
                String currentVersion = entry.getValue().getText();
                String newVersion = findNewerDependencyVersion(currentVersion).orElse(null);
                if (newVersion != null && !currentVersion.equals(newVersion)) {
                    entry = entry.withValue(entry.getValue().withText(newVersion));
                }
            }
            return super.visitEntry(entry, p);
        }

        private Optional<String> findNewerDependencyVersion(String currentVersion) {
            if (availableVersions == null) {
                MavenMetadata mavenMetadata = new MavenPomDownloader(MavenPomCache.NOOP,
                        emptyMap(), ctx).downloadMetadata(GROUP_ID, ARTIFACT_ID, emptyList());
                availableVersions = mavenMetadata.getVersioning().getVersions().stream()
                        .filter(versionComparator::isValid)
                        .collect(Collectors.toList());
            }

            LatestRelease latestRelease = new LatestRelease(null);
            return availableVersions.stream()
                    .filter(v -> latestRelease.compare(currentVersion, v) < 0)
                    .max(versionComparator);
        }
    }
}
