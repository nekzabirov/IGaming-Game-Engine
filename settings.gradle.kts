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
        maven {
            name = "GitHubPackagesUserEngine"
            url = uri("https://maven.pkg.github.com/nekzabirov/IGaming-User-Engine")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        maven {
            name = "GitHubPackagesWalletEngine"
            url = uri("https://maven.pkg.github.com/nekzabirov/IGaming-Wallet-Engine")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
