plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

group = "com.drl"
version = "1.0.0"

android {
    namespace = "com.drl.padlock"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compileOptions {
            targetCompatibility = JavaVersion.VERSION_21
        }
    }
}
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.drl.biologics"
                artifactId = "padlock"
                version = "1.0.0"
                from(components["release"])
                // Publish the release AAR
                artifact(file("libs/security-algorithm-1.0.0-beta.aar")) {
                    extension = "aar"
                    classifier = "extra"
                }
            }
            create<MavenPublication>("securityAlgorithm") {
                groupId = "com.drl.biologics"
                artifactId = "security-algorithm"
                version = "1.0.0-beta"

                artifact(file("libs/security-algorithm-1.0.0-beta.aar")) {
                    extension = "aar"
                }
            }
        }

        repositories {
            mavenLocal()
        }
    }
}
dependencies {
    compileOnly(fileTree("libs") { include("*.aar") })
    implementation(libs.androidx.core.ktx)
    implementation(libs.gson)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.bundles.tuya) {
        exclude(group = "com.thingclips.smart", module = "thingsmart-modularCampAnno")
    }
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}