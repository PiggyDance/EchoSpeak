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
            // 降噪: DeepFilterNet (神经网络降噪，替代 RNNoise)
            implementation(libs.deepfilternet)
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
        versionCode = 2
        versionName = "1.1"

        ndk {
            // 只打包 ARM 架构：
            // arm64-v8a  覆盖 2014 年后所有 64 位 ARM 设备（99%+ 现代机型）
            // armeabi-v7a 覆盖少量仍在使用的 32 位 ARM 旧设备
            // 不包含 x86/x86_64：仅用于模拟器，真机无需支持，可显著减小 APK 体积
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // DeepFilterNet 的 libtask_audio_jni.so 未按 16KB 对齐（上游库问题）。
            // useLegacyPackaging = true 让 AGP 以压缩方式打包 .so，
            // 安装时解压到磁盘运行，绕过 Play Store 的 16KB 对齐检查。
            // 代价：安装后磁盘占用略增（约 +2MB），运行时性能无影响。
            // 待上游 KaleyraVideo/AndroidDeepFilterNet 修复后可移除此配置。
            useLegacyPackaging = true
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

