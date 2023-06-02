plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Migrate between Micronaut versions. Automatically."

val rewriteVersion = if (project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}

dependencies {
    compileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")

    implementation(platform("org.openrewrite:rewrite-bom:${rewriteVersion}"))
    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-maven")
    implementation("org.openrewrite:rewrite-gradle")
    implementation("org.openrewrite:rewrite-properties")
    implementation("org.openrewrite:rewrite-yaml")
    runtimeOnly("org.openrewrite:rewrite-java-8")
    runtimeOnly("org.openrewrite:rewrite-java-11")
    runtimeOnly("org.openrewrite:rewrite-java-17")
    runtimeOnly("org.openrewrite.recipe:rewrite-migrate-java:${rewriteVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.openrewrite:rewrite-java-tck")
    testImplementation("org.assertj:assertj-core:latest.release")
    testRuntimeOnly(gradleApi())
}

recipeDependencies {
    parserClasspath("io.micronaut:micronaut-core:2.+")
    parserClasspath("io.micronaut:micronaut-context:2.+")
    parserClasspath("io.micronaut:micronaut-inject:2.+")
    parserClasspath("io.micronaut:micronaut-validation:2.+")
    parserClasspath("io.micronaut:micronaut-http:2.+")
    parserClasspath("io.micronaut:micronaut-http-server:2.+")
    parserClasspath("io.micronaut:micronaut-http-server-netty:2.+")
    parserClasspath("io.micronaut:micronaut-http-client:2.+")
    parserClasspath("io.micronaut:micronaut-http-client-core:2.+")
    parserClasspath("javax.inject:javax.inject:1")
    parserClasspath("jakarta.inject:jakarta.inject-api:2.+")
    parserClasspath("org.reactivestreams:reactive-streams:1.0.4")

    parserClasspath("io.micronaut:micronaut-context:4.0.0-M2")
    parserClasspath("io.micronaut:micronaut-websocket:4.0.0-M2")
    parserClasspath("io.micronaut.validation:micronaut-validation:4.0.0-M5")
    parserClasspath("io.micronaut:micronaut-retry:4.0.0-M4")
    parserClasspath("io.micronaut.email:micronaut-email:2.0.0-M1")
    parserClasspath("javax.annotation:javax.annotation-api:1.3.2")
    parserClasspath("javax.validation:validation-api:2.0.1.Final")
    parserClasspath("jakarta.validation:jakarta.validation-api:3.0.2")
    parserClasspath("javax.persistence:javax.persistence-api:2.2")
    parserClasspath("jakarta.persistence:jakarta.persistence-api:3.1.0")
    parserClasspath("javax.mail:javax.mail-api:1.6.2")
    parserClasspath("jakarta.mail:jakarta.mail-api:2.1.1")
    parserClasspath("jakarta.annotation:jakarta.annotation-api:2.1.1")
}
