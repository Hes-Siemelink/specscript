group = "hes.specscript"
version = "0.6.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
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
    implementation("io.ktor:ktor-client-core:3.0.+")
    implementation("io.ktor:ktor-client-java:3.0+")
    implementation("io.ktor:ktor-client-auth:3.0+")
    implementation("org.slf4j:slf4j-simple:2.0.+")
    implementation("com.github.kotlin-inquirer:kotlin-inquirer:0.1.0")
    implementation("org.jline:jline:3.27.+")
    implementation("org.fusesource.jansi:jansi:2.4.1")
    implementation("io.javalin:javalin:6.7.+")
    implementation("org.xerial:sqlite-jdbc:3.47.0.0")

    // Model Context Protocol dependencies
    implementation("io.modelcontextprotocol:kotlin-sdk:0.6.0")
    implementation("io.github.oshai:kotlin-logging:5.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
    testImplementation("net.pwall.json:json-kotlin-schema:0.47")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

sourceSets.main.get().resources.srcDir("instacli-spec")

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
        attributes["Main-Class"] = "instacli.cli.MainKt"
    }
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("full")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes["Main-Class"] = "instacli.cli.MainKt"
    }
    
    from(sourceSets.main.get().output)
    
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
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
    dependsOn(tasks.named("build"), tasks.named("fatJar"))
}

tasks.register("release") {
    dependsOn("clean", "githubRelease")
}