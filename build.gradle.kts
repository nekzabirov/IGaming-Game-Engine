import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
    application
    `maven-publish`
}

group = "com.nekgamebling"
version = "1.0.0"

val grpcClientVersionProvider = providers.gradleProperty("grpcClientVersion").orElse("0.0.1-SNAPSHOT")

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}

// Task to run the sync aggregators CLI
tasks.register<JavaExec>("runSync") {
    group = "application"
    description = "Run the sync all aggregators CLI"
    mainClass.set("SyncJobKt")
    classpath = sourceSets.main.get().runtimeClasspath
}

// Task to run the DB migration job (creates `casino` DB if missing, applies Flyway)
tasks.register<JavaExec>("runMigrate") {
    group = "application"
    description = "Run the DB migration job"
    mainClass.set("DbMigrateJobKt")
    classpath = sourceSets.main.get().runtimeClasspath
}

// Create additional start scripts for sync CLI
tasks.named<CreateStartScripts>("startScripts") {
    applicationName = "casino-engine"
}

val syncStartScripts by tasks.registering(CreateStartScripts::class) {
    applicationName = "sync-aggregators"
    mainClass.set("SyncJobKt")
    outputDir = layout.buildDirectory.dir("syncScripts").get().asFile
    classpath = tasks.named<Jar>("jar").get().outputs.files + configurations.runtimeClasspath.get()
}

val dbMigrateStartScripts by tasks.registering(CreateStartScripts::class) {
    applicationName = "db-migrate"
    mainClass.set("DbMigrateJobKt")
    outputDir = layout.buildDirectory.dir("dbMigrateScripts").get().asFile
    classpath = tasks.named<Jar>("jar").get().outputs.files + configurations.runtimeClasspath.get()
}

distributions {
    main {
        contents {
            from(syncStartScripts) {
                into("bin")
            }
            from(dbMigrateStartScripts) {
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

    // Redis
    implementation(libs.lettuce)

    // Flyway DB migrations
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // gRPC
    implementation(libs.bundles.grpc)
    implementation(libs.protobuf.kotlin)

    // Sibling gRPC clients published to GitHub Packages
    implementation("com.nekgambling:user-grpc-client:1.0.0")
    implementation("com.nekgamebling:wallet-grpc-client:1.0.0")

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.testcontainers)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
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

// ----------------------------------------------------------------------------
// gRPC client JAR — published to GitHub Packages as `com.nekgamebling:game-grpc-client`.
// Bundles only casino-engine's own generated proto/gRPC stubs (game.v1 package).
// Wallet stubs are consumed via the wallet-grpc-client artifact, not regenerated locally.
// ----------------------------------------------------------------------------
tasks.register<Jar>("grpcClientJar") {
    group = "build"
    description = "JAR with generated gRPC/proto classes for client consumers."
    archiveBaseName.set("game-grpc-client")
    archiveVersion.set(grpcClientVersionProvider)
    dependsOn(tasks.named("compileKotlin"), tasks.named("compileJava"))
    from(sourceSets.main.get().output.classesDirs) {
        include("com/nekgamebling/game/**")
    }
}

publishing {
    publications {
        create<MavenPublication>("grpcClient") {
            groupId = "com.nekgamebling"
            artifactId = "game-grpc-client"
            version = grpcClientVersionProvider.get()
            artifact(tasks.named("grpcClientJar"))

            pom.withXml {
                val deps = asNode().appendNode("dependencies")
                listOf(
                    Triple("io.grpc", "grpc-stub", "1.68.2"),
                    Triple("io.grpc", "grpc-protobuf", "1.68.2"),
                    Triple("io.grpc", "grpc-kotlin-stub", "1.4.1"),
                    Triple("com.google.protobuf", "protobuf-java", "4.29.2"),
                    Triple("com.google.protobuf", "protobuf-kotlin", "4.29.2"),
                ).forEach { (groupId, artifactId, version) ->
                    val dep = deps.appendNode("dependency")
                    dep.appendNode("groupId", groupId)
                    dep.appendNode("artifactId", artifactId)
                    dep.appendNode("version", version)
                    dep.appendNode("scope", "compile")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "OWNER/REPO"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

