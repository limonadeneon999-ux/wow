plugins {
    alias(libs.plugins.protobuf)
}

extension {
    name = "extensions/shared.mpe"
}

// The plugin renames every DEX to shared.mpe. Preserve secondary DEX files separately
// so the patcher can merge each one using its existing single-DEX extension API.
tasks.named<Sync>("syncExtension") {
    eachFile {
        val suffix = file.name.removePrefix("classes").removeSuffix(".dex")
        path = "shared$suffix.mpe"
    }
}

android {
    namespace = "app.morphe.extension"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    // android.jar only contains stubs that throw at runtime, and AGP 9 no longer
    // produces the mockable jar. Returning default values lets framework calls
    // (android.util.Log, for instance) run as no-ops instead of throwing.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.mockito.core)
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation("org.json:json:20240303")
    compileOnly(libs.annotation)
    compileOnly(libs.preference)

    implementation(libs.androidx.javascriptengine)
    implementation(libs.collections4)
    implementation(libs.gson)
    implementation(libs.lang3)
    implementation(libs.okhttp3)

    implementation(libs.nanohttpd)
    implementation(libs.protobuf.javalite)

    implementation(files("libs/jump3r-1.0.5.jar"))

    implementation(libs.hiddenapi)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    compileOnly(project(":extensions:shared:stub"))

    // JVM tests run without the Android framework. The lyrics tests build a
    // Context double that instantiates android.test.mock.MockPackageManager, which
    // lives in the SDK's optional jars rather than in android.jar.
    val sdkPlatform = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    if (sdkPlatform != null) {
        testImplementation(files("$sdkPlatform/platforms/android-35/optional/android.test.mock.jar"))
        testImplementation(files("$sdkPlatform/platforms/android-35/optional/android.test.base.jar"))
    }
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
