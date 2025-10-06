group = "hes.specscript"
version = "0.6.1"

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
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

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.+")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.+")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.+")
    implementation("com.networknt:json-schema-validator:1.4.0")
    implementation("io.ktor:ktor-client-core:3.3.+")
    implementation("io.ktor:ktor-client-java:3.3+")
    implementation("io.ktor:ktor-client-auth:3.3+")
    implementation("io.ktor:ktor-server-core:3.3.+")
    implementation("io.ktor:ktor-server-netty:3.3.+")
    implementation("io.ktor:ktor-server-sse:3.3.+")
    implementation("org.slf4j:slf4j-simple:2.0.+")
    implementation("com.github.kotlin-inquirer:kotlin-inquirer:0.1.0")
    implementation("org.jline:jline:3.27.+")
    implementation("org.fusesource.jansi:jansi:2.4.1")
    // Removed Javalin dependency after migrating HttpServer to Ktor
    implementation("org.xerial:sqlite-jdbc:3.47.0.0")

    // Model Context Protocol dependencies
    implementation("io.modelcontextprotocol:kotlin-sdk:0.7.2")
    implementation("io.github.oshai:kotlin-logging:5.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
    testImplementation("net.pwall.json:json-kotlin-schema:0.47")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

sourceSets.main.get().resources.srcDir("specification")

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()

            sources {
                java {
                    setSrcDirs(listOf("src/tests/unit"))
                }
            }
        }

        register<JvmTestSuite>("specificationTest") {

            dependencies {
                implementation(project())
                implementation("io.kotest:kotest-assertions-core:5.7.2")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.+")
                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.+")
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
    body = "Release of SpecScript ${project.version}"
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