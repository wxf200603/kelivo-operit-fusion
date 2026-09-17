import java.util.Properties

plugins {
    id("com.android.application")
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.kelivo.operit.fusion"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    defaultConfig {
        applicationId = "com.kelivo.operit.fusion"
        minSdk = 26
        targetSdk = 36
        versionCode = flutter.versionCode
        versionName = flutter.versionName
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    val keystorePropertiesFile = rootProject.file("key.properties")
    val keystoreProperties = Properties()
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

flutter {
    source = "../.."
}

val requiredProotLibs = listOf(
    "arm64-v8a/libproot_exec.so",
    "arm64-v8a/libproot_loader.so",
    "arm64-v8a/libtalloc.so",
    "arm64-v8a/libandroid-shmem.so",
)

tasks.register<Exec>("fetchProot") {
    val repoRoot = rootProject.projectDir.parentFile
    commandLine("bash", repoRoot.resolve("tool/fetch_proot.sh").absolutePath)
    workingDir = repoRoot
    onlyIf {
        val jniLibs = layout.projectDirectory.dir("src/main/jniLibs")
        requiredProotLibs.any { name ->
            val so = jniLibs.file(name).asFile
            !so.isFile || so.length() == 0L
        }
    }
}

tasks.whenTaskAdded {
    if (name == "preBuild") {
        dependsOn("fetchProot")
    }
}
tasks.findByName("preBuild")?.dependsOn("fetchProot")

dependencies {
    implementation("androidx.browser:browser:1.9.0")
    implementation("org.tukaani:xz:1.10")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")

    // Operit 核心依赖
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.7.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Shizuku
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // Root access
    implementation("com.github.topjohnwu.libsu:core:6.0.0")
    implementation("com.github.topjohnwu.libsu:service:6.0.0")
    implementation("com.github.topjohnwu.libsu:nio:6.0.0")

    // MCP
    implementation("io.modelcontextprotocol:kotlin-sdk-client:0.10.0")
}
