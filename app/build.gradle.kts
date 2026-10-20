plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.uptrendinvestments.clickmeapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.uptrendinvestments.clickmeapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "env"
    productFlavors {
        // Day-to-day flavor: the backend Stripe account is in test mode, so the pk_test key is the one that works (D-05).
        create("dev") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://clickme-backend-production.up.railway.app/api/v1\"")
            buildConfigField(
                "String",
                "STRIPE_PUBLISHABLE_KEY",
                "\"pk_test_51Tha0rHGu8AnfRoUiTqYX1Kt1QJ5bvN25ONTnckKYOeT53y8FrVWbXcyNdZfIL2U5FsuLP35OVTmqqO1Vwy0SzJm00I3w6Mn51\"",
            )
            buildConfigField("String", "AGORA_APP_ID", "\"483eaa4d11fb44b6acc91bf5ceae36c7\"")
        }
        // The pk_live key below only works after the backend switches Stripe to live mode at launch (Phase 17 release item, D-05).
        create("prod") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://clickme-backend-production.up.railway.app/api/v1\"")
            buildConfigField(
                "String",
                "STRIPE_PUBLISHABLE_KEY",
                "\"pk_live_51U8OTJENKN7xExknds5a5qHfLfajVnkJQ8lli0GmQYVcP6VoT8tDLjBpXqBtqO5Bv1ijaVwTm5d3MSKhI7Vl4fSm009vGo9aEa\"",
            )
            buildConfigField("String", "AGORA_APP_ID", "\"483eaa4d11fb44b6acc91bf5ceae36c7\"")
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    lint {
        abortOnError = true
        warningsAsErrors = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
