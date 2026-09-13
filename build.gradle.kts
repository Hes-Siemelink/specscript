group = "hes.specscript"
version = "0.12.1-SNAPSHOT"

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    `maven-publish`
    id("com.github.breadmoirai.github-release") version "2.5.2"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("tools.jackson.module:jackson-module-kotlin:3.2.1")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.2.1")
    implementation("com.networknt:json-schema-validator:3.0.+")
    implementation("io.ktor:ktor-client-core:3.5.2")
    implementation("io.ktor:ktor-client-java:3.5.2")
    implementation("io.ktor:ktor-client-auth:3.5.2")
    implementation("io.ktor:ktor-server-core:3.5.2")
    implementation("io.ktor:ktor-server-netty:3.5.2")
    implementation("io.ktor:ktor-server-sse:3.5.2")
    implementation("ch.qos.logback:logback-classic:1.6.3")
    implementation("com.github.kotlin-inquirer:kotlin-inquirer:0.1.0")
    implementation("org.jline:jline:3.30.17")
    implementation("org.fusesource.jansi:jansi:2.4.3")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("io.modelcontextprotocol:kotlin-sdk:0.15.0")
    implementation("io.ktor:ktor-server-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.2")
    implementation("io.github.oshai:kotlin-logging:8.0.4")  // For MCP

    implementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.3")

    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
}

sourceSets.main.get().resources.srcDir("specification")

testing {
    suites {
        val test = getByName<JvmTestSuite>("test") {
            useJUnitJupiter("6.1.3") // Same dependency for the test suite as the main code base to avoid conflicts

            sources {
                java {
                    setSrcDirs(listOf("src/tests/unit"))
                }
            }
        }

        register<JvmTestSuite>("specificationTest") {

            useJUnitJupiter("6.1.3") // Same dependency for the test suite as the main code base to avoid conflicts

            dependencies {
                implementation(project())
                implementation("io.kotest:kotest-assertions-core:5.7.2")
                implementation("tools.jackson.module:jackson-module-kotlin:3.2.1")
                implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.2.1")
            }

            sources {
                java {
                    setSrcDirs(listOf("src/tests/specification"))
                }
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }

    }
}


tasks.named("check") {
    dependsOn(testing.suites.named("specificationTest"))
}

//
// TypeScript tests
//

tasks.register<Exec>("typescriptTest") {
    group = "verification"
    description = "Runs the TypeScript implementation tests"
    workingDir = file("typescript")
    commandLine("sh", "-c", "pnpm test")
}

tasks.register("checkAll") {
    group = "verification"
    description = "Runs all checks including TypeScript tests"
    dependsOn("check", "typescriptTest")
}

//
// Executable jar file
//

tasks.jar {
    manifest {
        attributes["Main-Class"] = "specscript.cli.MainKt"
    }
}

tasks.register<Jar>("fullJar") {
    archiveClassifier.set("full")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "specscript.cli.SpecScriptCliKt"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

// Make sure the standard build produces the fat jar
tasks.named("build") {
    dependsOn("fullJar")
}

//
// Release
//

githubRelease {
    token(System.getenv("GITHUB_TOKEN"))
    repo = "specscript"
    owner = "Hes-Siemelink"
    tagName = "${project.version}"
    releaseName = "SpecScript ${project.version}"
    targetCommitish = "main"
    body = project.findProperty("releaseHeadline")?.toString()
        ?: "Release of SpecScript ${project.version}"
    draft = false
    prerelease = false
    overwrite = true
    releaseAssets(
        file("build/libs/specscript-${project.version}.jar"),
        file("build/libs/specscript-${project.version}-full.jar")
    )
}

tasks.named("githubRelease") {
    dependsOn(tasks.named("build"), tasks.named("fullJar"))
}

tasks.register("release") {
    dependsOn("clean", "githubRelease")
}

//
// Library publishing
//

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

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

    repositories {
        mavenLocal()
    }
}
