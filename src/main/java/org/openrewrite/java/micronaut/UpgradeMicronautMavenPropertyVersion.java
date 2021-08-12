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
import org.openrewrite.maven.ChangePropertyValue;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.semver.Semver;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeMicronautMavenPropertyVersion extends Recipe {

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
        return "Set the maven micronaut.version property according to a node-style semver selector or to a specific version number.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UpgradeMicronautMavenVersionVisitor(newVersion);
    }

    private static class UpgradeMicronautMavenVersionVisitor extends MavenVisitor {
        private final String newVersion;

        public UpgradeMicronautMavenVersionVisitor(String newVersion) {
            this.newVersion = newVersion;
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            Maven mvn = super.visitMaven(maven, ctx);
            String currentVersion = mvn.getModel().getProperties().get("micronaut.version");
            if (currentVersion != null && !currentVersion.isEmpty()) {
                MicronautVersionHelper.getNewerVersion(newVersion, currentVersion, ctx)
                        .ifPresent(latestVersion -> doAfterVisit(new ChangePropertyValue("micronaut.version", latestVersion)));
            }
            return mvn;
        }
    }
}
