import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            // 启动框架: Android Startup
            implementation(libs.android.startup)
            // 广告框架: Google Play Services Ads
            implementation(libs.play.services.ads)
            // VAD: Voice Activity Detection (Silero / WebRTC / YAMNet)
            implementation(libs.silero.vad)
            implementation(libs.webrtc.vad)
            implementation(libs.yamnet.vad)
            // 权限处理: Accompanist Permissions
            implementation(libs.accompanist.permissions)
            // Google Sign-In: Credential Manager
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play)
            implementation(libs.googleid)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Koin 依赖
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            // piggydance 的基础库
            implementation(projects.basicModules.basicDeps)
            implementation(projects.businessModules.speechEcho)
            // 图标库: Compose Icons
            implementation(libs.composeIcons.cssGg)
            implementation(libs.composeIcons.weatherIcons)
            implementation(libs.composeIcons.evaIcons)
            implementation(libs.composeIcons.feather)
            implementation(libs.composeIcons.fontAwesome)
            implementation(libs.composeIcons.lineAwesome)
            implementation(libs.composeIcons.linea)
            implementation(libs.composeIcons.octicons)
            implementation(libs.composeIcons.simpleIcons)
            implementation(libs.composeIcons.tablerIcons)
            // Lottie Compottie
            implementation(libs.compottie)
            implementation(libs.compottieDot)
            implementation(libs.compottieNetwork)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "io.piggydance.echospeak"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.piggydance.echospeak"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // NDK：RNNoise JNI 共享库，只编译主流 ABI
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }

        // CMake：指向 RNNoise + JNI 胶水层的构建脚本
        externalNativeBuild {
            cmake {
                cppFlags("")
                arguments("-DANDROID_STL=c++_shared")
            }
        }
    }

    // 关联 CMakeLists.txt 位置
    externalNativeBuild {
        cmake {
            path = file("src/androidMain/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

