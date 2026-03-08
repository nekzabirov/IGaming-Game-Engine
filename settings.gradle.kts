rootProject.name = "casino-engine"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

includeBuild("../wallete-engine/wallet-grpc-client") {
    dependencySubstitution {
        substitute(module("com.nekgamebling:wallet-grpc-client")).using(project(":"))
    }
}
