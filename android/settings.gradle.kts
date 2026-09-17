pluginManagement {
    val flutterSdkPath = run {
        val fromEnv = System.getenv("FLUTTER_ROOT")
        if (fromEnv != null) fromEnv
        else {
            val properties = java.util.Properties()
            val localProps = file("local.properties")
            if (localProps.exists()) {
                localProps.inputStream().use { properties.load(it) }
            }
            val fromProps = properties.getProperty("flutter.sdk")
            if (fromProps != null) fromProps
            else {
                val flutterCmd = System.getenv("PATH")?.split(File.pathSeparator)?.map { File(it, "flutter") }?.firstOrNull { it.exists() }
                if (flutterCmd != null) flutterCmd.parentFile.parentFile.absolutePath
                else error("Flutter SDK not found. Set FLUTTER_ROOT env var or flutter.sdk in local.properties")
            }
        }
    }

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
}

include(":app")
