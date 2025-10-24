group = "hes.specscript"
version = "0.6.2-SNAPSHOT"

plugins {
    kotlin("multiplatform") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    `maven-publish`
    id("com.github.breadmoirai.github-release") version "2.5.2"
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "21"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    // Minimal native target (can expand later). Assumes Apple Silicon; adjust if needed.
    macosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.github.oshai:kotlin-logging:5.0.0")
                implementation("io.ktor:ktor-client-core:3.3.+")
                // Serialization runtime (recommended for multiplatform)
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-assertions-core:5.7.2")
            }
        }
        val jvmMain by getting {
            // Reuse existing JVM sources without moving immediately
            kotlin.srcDir("src/main/kotlin")
            resources.srcDir("src/main/resources")
            // Include specification directory as resources (original line migrated)
            resources.srcDir("specification")
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.+")
                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.+")
                implementation("com.networknt:json-schema-validator:1.4.0")
                implementation("io.ktor:ktor-client-java:3.3+")
                implementation("io.ktor:ktor-client-auth:3.3+")
                implementation("io.ktor:ktor-server-core:3.3.+")
                implementation("io.ktor:ktor-server-netty:3.3.+")
                implementation("io.ktor:ktor-server-sse:3.3.+")
                implementation("org.slf4j:slf4j-simple:2.0.+")
                implementation("com.github.kotlin-inquirer:kotlin-inquirer:0.1.0")
                implementation("org.jline:jline:3.27.+")
                implementation("org.fusesource.jansi:jansi:2.4.1")
                implementation("org.xerial:sqlite-jdbc:3.47.0.0")
                implementation("io.modelcontextprotocol:kotlin-sdk:0.7.2")
            }
        }
        val jvmTest by getting {
            kotlin.srcDir("src/test/kotlin")
            kotlin.srcDir("src/tests/unit")
            kotlin.srcDir("src/tests/specification")
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
                implementation("io.kotest:kotest-assertions-core:5.7.2")
                implementation("net.pwall.json:json-kotlin-schema:0.47")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.+")
                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.+")
            }
        }
        val macosArm64Main by getting {
            // Native-specific deps can go here later
        }
        val macosArm64Test by getting {
            dependencies { implementation(kotlin("test")) }
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

// Keep specificationTest concept via a separate task grouping (lightweight adaptation)
// Original custom test suite retained by source set wiring above.

// Fat jar using JVM runtime classpath from multiplatform setup
val jvmRuntimeClasspath = configurations.getByName("jvmRuntimeClasspath")
val jvmJarTask = tasks.named("jvmJar")

tasks.register<Jar>("fullJar") {
    archiveClassifier.set("full")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = "specscript.cli.SpecScriptCliKt" }
    // Unpack compiled JVM jar instead of accessing kotlin.targets (deprecated)
    dependsOn(jvmJarTask)
    from({ zipTree(jvmJarTask.get().outputs.files.singleFile) })
    // Include runtime dependencies
    dependsOn(jvmRuntimeClasspath)
    from({ jvmRuntimeClasspath.filter { it.name.endsWith(".jar") }.map { zipTree(it) } })
}

// Ensure standard build produces fat jar
tasks.named("build") { dependsOn("fullJar") }

// Release configuration unchanged
githubRelease {
    token(System.getenv("GITHUB_TOKEN"))
    repo = "specscript"
    owner = "Hes-Siemelink"
    tagName = "${project.version}"
    releaseName = "SpecScript ${project.version}"
    targetCommitish = "main"
    body = "Release of SpecScript ${project.version}"
    draft = false
    prerelease = false
    overwrite = true
    releaseAssets(
        file("build/libs/specscript-${project.version}.jar"),
        file("build/libs/specscript-${project.version}-full.jar")
    )
}

tasks.named("githubRelease") { dependsOn(tasks.named("build"), tasks.named("fullJar")) }

tasks.register("release") { dependsOn("clean", "githubRelease") }

publishing {
    publications {
        // Publish JVM artifacts explicitly
        create<MavenPublication>("mavenJvm") {
            artifact(tasks.named("jvmJar"))
            artifact(tasks.named("fullJar")) // fat jar with classifier "full"
            pom {
                name.set("SpecScript")
                description.set("Spec your projects the human and AI-friendly way using Markdown and Yaml")
                url.set("https://github.com/Hes-Siemelink/specscript")
                licenses {
                    license {
                        name.set("Custom License - View Only")
                        url.set("https://github.com/Hes-Siemelink/specscript/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("Hes-Siemelink")
                        name.set("Hes Siemelink")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Hes-Siemelink/specscript.git")
                    developerConnection.set("scm:git:ssh://github.com/Hes-Siemelink/specscript.git")
                    url.set("https://github.com/Hes-Siemelink/specscript")
                }
            }
        }
    }
    repositories { mavenLocal() }
}

// Alias 'test' task for convenience (maps to JVM tests for now)
tasks.register("test") {
    group = "verification"
    description = "Runs JVM tests (alias for jvmTest)."
    dependsOn("jvmTest")
}

// Ensure 'check' aggregates all target tests
tasks.named("check") { dependsOn("allTests") }
