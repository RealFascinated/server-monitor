import org.gradle.kotlin.dsl.developmentOnly
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
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

    // Postgres
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.security:spring-security-crypto")

    compileOnly("org.jetbrains:annotations:26.0.2")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<BootRun>("bootRun") {
    val workDir = layout.projectDirectory.dir("work")
    workingDir = workDir.asFile
    doFirst { workDir.asFile.mkdirs() }
}

tasks.withType<Test> {
    useJUnitPlatform()
    enabled = false
}
