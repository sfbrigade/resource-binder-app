import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
        }
    }
    
    android {
       namespace = "org.sfcivictech.android.shared.resourcebinder.sharedLogic"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
           // model_int8.onnx (~22MB) lives under src/androidMain/assets/ --
           // bundled into the AAR/APK, loaded offline via AssetManager at
           // runtime. See search/embedding/OnnxEmbeddingModel.kt.
           noCompress += "onnx"
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTest {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        commonMain.dependencies {
            // put your Multiplatform dependencies here
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
            implementation(libs.sqldelight.android.driver)

            // On-device semantic search (org.sfcivictech.android.shared.resourcebinder.search):
            // ONNX Runtime for Android specifically (not the desktop
            // `onnxruntime` artifact) -- compiled against Android's
            // ART/NDK ABI, with NNAPI/XNNPACK execution providers
            // available. No tokenizer dependency:
            // search/embedding/WordPieceTokenizer.kt is pure Kotlin --
            // ai.djl.huggingface:tokenizers' native binding only ships
            // desktop-platform .so/.dylib/.dll, not an Android-ABI build,
            // and UnsatisfiedLinkError's at runtime on-device.
            implementation(libs.onnxruntime.android)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.commons.csv)
        }
        val androidDeviceTest by getting {
            dependencies {
                implementation(libs.androidx.testExt.junit)
                implementation(libs.androidx.test.runner)
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}