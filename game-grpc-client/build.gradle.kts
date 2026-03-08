import com.google.protobuf.gradle.id

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.google.protobuf") version "0.9.4"
    `maven-publish`
}

group = "com.nekgamebling"
version = project.findProperty("grpcClientVersion") ?: "1.0.0"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

val grpcVersion = "1.68.2"
val grpcKotlinVersion = "1.4.1"
val protobufVersion = "4.29.2"

dependencies {
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}

sourceSets {
    main {
        proto {
            srcDir("../src/main/proto")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "game-grpc-client"
            from(components["java"])
            pom {
                name.set("Game-Core gRPC Client")
                description.set("gRPC client stubs for Game Core Service")
            }
        }
    }
    repositories {
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
