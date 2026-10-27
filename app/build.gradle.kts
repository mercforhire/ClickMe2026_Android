import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

// Release signing is local only (D-15): CI has no keystore.properties and builds unsigned debug variants.
// The upload key is separate from the Google-generated Play App Signing key (D-16).
// Read through providers.fileContents so the configuration cache tracks the file appearing or changing.
// Never print property values.
val keystorePropsText =
    providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties")).asText
val keystoreProps =
    Properties().apply {
        keystorePropsText.orNull?.let { load(it.reader()) }
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
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "env"
    productFlavors {
        // Day-to-day flavor. The STRIPE_PUBLISHABLE_KEY below must be the publishable key of the same Stripe account
        // and mode as the backend's secret key. The current pk_test_51Tha0r... value belongs to a different account
        // (Phase 2 UAT tests 7-8); the swap is Phase 7 D-01/D-02 (plan 07-05).
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

    signingConfigs {
        if (!keystoreProps.isEmpty) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (!keystoreProps.isEmpty) signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.stripe.paymentsheet)
    implementation(libs.agora.voice.sdk)
    implementation(libs.moshi)
    // Android provides org.json; the 2009 jar would duplicate it.
    implementation(libs.socket.io.client) { exclude(group = "org.json", module = "json") }
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver3)
}
