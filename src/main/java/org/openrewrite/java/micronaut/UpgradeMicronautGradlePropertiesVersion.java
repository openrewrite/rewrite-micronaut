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
import org.openrewrite.*;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.Semver;

import java.time.Duration;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeMicronautGradlePropertiesVersion extends Recipe {

    private static final String PROPERTY_KEY = "micronautVersion";
    private static final String FILE_MATCHER = "**/gradle.properties";

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
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ChangePropertyValueVisitor(newVersion);
    }

    private static class ChangePropertyValueVisitor extends PropertiesVisitor<ExecutionContext> {

        private final String newVersion;

        public ChangePropertyValueVisitor(String newVersion) {
            this.newVersion = newVersion;
        }

        @Override
        public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
            if (entry.getKey().equals(PROPERTY_KEY)) {
                String currentVersion = entry.getValue().getText();
                String latestVersion = null;
                try {
                    latestVersion = MicronautVersionHelper.getNewerVersion(newVersion, currentVersion, ctx).orElse(null);
                } catch (MavenDownloadingException e) {
                    return Markup.warn(entry, e);
                }
                if (latestVersion != null && !currentVersion.equals(latestVersion)) {
                    entry = entry.withValue(entry.getValue().withText(latestVersion));
                }
            }
            return super.visitEntry(entry, ctx);
        }
    }
}
