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
    mainClass.set("ApplicationKt")
}

// Task to run the sync aggregators CLI
tasks.register<JavaExec>("runSync") {
    group = "application"
    description = "Run the GameHub catalog sync CLI"
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
    applicationName = "sync-catalog"
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
    implementation(libs.bundles.ktor.server)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Database - Exposed ORM over HikariCP; Flyway owns the schema
    implementation(libs.bundles.exposed)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    // Messaging - RabbitMQ (events out, one consumer in)
    implementation(libs.amqp.client)

    // Redis - player limits
    implementation(libs.lettuce)
    implementation(libs.kotlinx.coroutines.reactive)

    // gRPC - our own services + GameHub client
    implementation(libs.bundles.grpc)
    implementation(libs.protobuf.kotlin)

    // pam-engine: player account, wallet ledger and currency registry in one artifact
    implementation("com.nekgambling:pam-grpc-client:1.0.0")

    implementation(libs.logback)

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

