import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
    `maven-publish`
    application
}

group = "com.nekgamebling"
version = "1.0.0"

val grpcClientVersion: String by project

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.nekgamebling.MainKt")
}

// Task to run the sync aggregators CLI
tasks.register<JavaExec>("runSync") {
    group = "application"
    description = "Run the sync all aggregators CLI"
    mainClass.set("com.nekgamebling.SyncMainKt")
    classpath = sourceSets.main.get().runtimeClasspath
}

// Create additional start scripts for sync CLI
tasks.named<CreateStartScripts>("startScripts") {
    applicationName = "game-core"
}

val syncStartScripts by tasks.registering(CreateStartScripts::class) {
    applicationName = "sync-aggregators"
    mainClass.set("com.nekgamebling.SyncMainKt")
    outputDir = layout.buildDirectory.dir("syncScripts").get().asFile
    classpath = tasks.named<Jar>("jar").get().outputs.files + configurations.runtimeClasspath.get()
}

distributions {
    main {
        contents {
            from(syncStartScripts) {
                into("bin")
            }
        }
    }
}

tasks.named("build") {
    finalizedBy("installDist")
}

dependencies {
    // Ktor Server
    implementation(libs.bundles.ktor.server)

    // Ktor Client
    implementation(libs.bundles.ktor.client)

    // Serialization
    implementation(libs.ktor.serialization.json)

    // Database - Exposed ORM
    implementation(libs.bundles.exposed)
    implementation(libs.h2)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    // Dependency Injection - Koin
    implementation(libs.bundles.koin)

    // Logging
    implementation(libs.logback)

    // DateTime
    implementation(libs.kotlinx.datetime)

    // Messaging - RabbitMQ
    implementation(libs.rabbitmq)

    // AWS S3
    implementation(libs.aws.s3)

    // gRPC
    implementation(libs.bundles.grpc)
    implementation(libs.protobuf.kotlin)

    // Testing
    testImplementation(libs.bundles.testing)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.29.2"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.68.2"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}

// gRPC Client JAR - contains only generated proto stubs for external services
val grpcClientJar by tasks.registering(Jar::class) {
    archiveBaseName.set("game-grpc-client")
    archiveVersion.set(grpcClientVersion)
    archiveClassifier.set("")

    dependsOn("compileKotlin", "compileJava")

    // Include compiled proto classes from the main output
    from(sourceSets.main.get().output) {
        // Include all generated proto packages (com.nekgamebling.game.*)
        include("com/nekgamebling/game/**")
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Sources JAR for the gRPC client
val grpcClientSourcesJar by tasks.registering(Jar::class) {
    archiveBaseName.set("game-grpc-client")
    archiveVersion.set(grpcClientVersion)
    archiveClassifier.set("sources")

    dependsOn("generateProto")

    from("src/main/proto") {
        include("**/*.proto")
    }
    from(layout.buildDirectory.dir("generated/source/proto/main/java"))
    from(layout.buildDirectory.dir("generated/source/proto/main/kotlin"))
    from(layout.buildDirectory.dir("generated/source/proto/main/grpc"))
    from(layout.buildDirectory.dir("generated/source/proto/main/grpckt"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Versions for POM dependencies (sync with libs.versions.toml)
val grpcVersion = "1.68.2"
val grpcKotlinVersion = "1.4.1"
val protobufVersion = "4.29.2"

publishing {
    publications {
        create<MavenPublication>("grpcClient") {
            groupId = "com.nekgamebling"
            artifactId = "game-grpc-client"
            version = grpcClientVersion

            artifact(grpcClientJar)
            artifact(grpcClientSourcesJar)

            pom {
                name.set("Game-Core gRPC Client")
                description.set("gRPC client stubs for Game Core Service")

                withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")

                    // gRPC runtime dependencies needed by clients
                    mapOf(
                        "io.grpc" to mapOf(
                            "grpc-stub" to grpcVersion,
                            "grpc-protobuf" to grpcVersion,
                            "grpc-kotlin-stub" to grpcKotlinVersion
                        ),
                        "com.google.protobuf" to mapOf(
                            "protobuf-kotlin" to protobufVersion
                        ),
                        "org.jetbrains.kotlinx" to mapOf(
                            "kotlinx-coroutines-core" to "1.9.0"
                        )
                    ).forEach { (groupId, artifacts) ->
                        artifacts.forEach { (artifactId, version) ->
                            val depNode = dependenciesNode.appendNode("dependency")
                            depNode.appendNode("groupId", groupId)
                            depNode.appendNode("artifactId", artifactId)
                            depNode.appendNode("version", version)
                            depNode.appendNode("scope", "compile")
                        }
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "nekzabirov/igambling-core"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String? ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.token") as String? ?: ""
            }
        }
    }
}