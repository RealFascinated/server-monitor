plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "cc.fascinated"
version = "0.0.1-SNAPSHOT"
description = "API"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.jenkins-ci.org/public/") }
}

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Websockets
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // Postgres
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.security:spring-security-crypto")

    // Libraries
    implementation("com.pngencoder:pngencoder:0.16.0")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("net.jodah:expiringmap:0.5.11")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.6.1")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.4.2")
    implementation("com.squareup.okhttp3:okhttp-urlconnection:5.3.2")
    implementation("org.kohsuke:github-api:2.0-rc.6")

    // Adventure API
    implementation("net.kyori:adventure-text-serializer-gson:4.26.1")
    implementation("net.kyori:adventure-text-serializer-legacy:4.26.1")
    implementation("com.google.guava:guava:33.6.0-jre")

    // DNS Lookup
    implementation("com.github.dnsjava:dnsjava:v3.5.2")

    // SwaggerUI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")

    // GeoIP - IP Lookups
    implementation("com.maxmind.geoip2:geoip2:5.0.2")

    // Archive Utilities
    implementation("org.codehaus.plexus:plexus-archiver:4.11.0")

    // File Storage
    implementation("io.minio:minio:8.6.0")

    // Metrics
    implementation("io.prometheus:prometheus-metrics-core:1.6.1")
    implementation("io.prometheus:prometheus-metrics-exposition-formats:1.6.1")
    implementation("io.prometheus:prometheus-metrics-model:1.6.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    enabled = false
}
