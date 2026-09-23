plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    // The Kotlin package root, NOT the store identity. docs/BRAND.md keeps internal module
    // names as they are; renaming this would rewrite the package line of every file in the
    // repo and change nothing a user or a store can see.
    namespace = "com.resumemaster.android"
    compileSdk = 37

    defaultConfig {
        // ⛔ IMMUTABLE AFTER FIRST PUBLISH, and this app is not published. Decided in
        // docs/BRAND.md. ⚠ Availability in the Play Store was NOT verified from here and must
        // be checked before first publish; the fallback is com.jobsviadraft.android.
        applicationId = "com.draft.android"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // AppGraph reads BuildConfig.DEBUG to choose between the local server and production.
        // Off by default since AGP 8, and its absence is a compile error rather than a wrong value.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/native-image/**"
        }
    }
}

// Room writes the schema JSON to app/schemas/ so a future migration can be written against a real
// baseline. Without it Room warns and exports nothing, and the first migration would have to be
// reconstructed from whatever the entity classes look like by then.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.itext.core)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.security.crypto)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

