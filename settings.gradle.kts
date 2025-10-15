rootProject.name = "EchoSpeak"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()

        // 火山引擎
        maven(url = "https://artifact.bytedance.com/repository/Volcengine/")
        maven(url = "https://artifact.bytedance.com/repository/pangle/")
    }
}

include(":composeApp")
// 基础模块
include(":basic-modules:basic-deps")
include(":basic-modules:common-ads")
include(":basic-modules:common-settings")
// 业务模块
include(":business-modules:speech-echo")
