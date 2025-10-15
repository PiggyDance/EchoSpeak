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
include(":business-modules:speech-echo")
include(":basic-modules:common-ads")
include(":basic-modules:common-settings")
