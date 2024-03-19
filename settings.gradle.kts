
import java.util.Properties
import java.io.File

val properties = Properties()
val localProps = File(rootDir.absolutePath, "local.properties")
if (localProps.exists()) {
    properties.load(localProps.inputStream())
} else {
    println("local.properties not found")
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/trustwallet/wallet-core")
            credentials {
                username = properties["gpr.user"] as? String
                password = properties["gpr.key"] as? String
            }
        }
    }
}

rootProject.name = "XplaAndroid"
include(":app")
