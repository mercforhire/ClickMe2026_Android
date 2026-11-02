import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

// google-services.json is gitignored (D-20), so CI and worktrees build without it. A release made without it
// compiles but has no push (Pitfall 10), hence the warning. Never commit a dummy copy.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("google-services.json is missing: this build has no push notifications (see 13-14 release checklist)")
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

// The production backend. The prod flavor always uses it and never reads local.properties.
val productionBaseUrl = "https://clickme-backend-production.up.railway.app/api/v1"

// Dev flavor only: `clickme.devBaseUrl` in the gitignored local.properties points the dev build at a local test
// backend (see .planning/phases/07-payment-methods-paid-booking/07-LOCAL-STRIPE-TESTING.md). Read through
// providers.fileContents like the keystore above. The value is a URL, not a secret.
val localPropsText =
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText
val localProps =
    Properties().apply {
        localPropsText.orNull?.let { load(it.reader()) }
    }
val devBaseUrl: String =
    localProps
        .getProperty("clickme.devBaseUrl")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.also { override ->
            // Rejects anything that could break out of the generated BuildConfig string literal.
            if (!Regex("""^https?://[^\s"\\]+$""").matches(override)) {
                throw GradleException(
                    "clickme.devBaseUrl in local.properties must be an http(s) URL without spaces, quotes or backslashes",
                )
            }
            logger.warn("The dev BASE_URL comes from clickme.devBaseUrl in local.properties: $override")
            val host = URI(override).host
            if (override.startsWith("http://") && host != "localhost" && host != "127.0.0.1") {
                logger.warn(
                    "clickme.devBaseUrl is http:// but the debug network security config allows cleartext only " +
                        "to localhost and 127.0.0.1",
                )
            }
        } ?: productionBaseUrl

android {
    namespace = "com.uptrendinvestments.clickmeapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.uptrendinvestments.clickmeapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "env"
    productFlavors {
        // Day-to-day flavor. The STRIPE_PUBLISHABLE_KEY below (pk_test_51Tha0...) is the publishable key of Stripe
        // sandbox account acct_1Tha0rHGu8AnfRoU, which the backend confirmed on 2026-10-28. It only works against a
        // backend that runs that sandbox's secret key, that is the local test backend (clickme.devBaseUrl, see
        // .planning/phases/07-payment-methods-paid-booking/07-LOCAL-STRIPE-TESTING.md). Against the default
        // production BASE_URL, which has been live mode since 2026-10-17, Stripe calls from the dev flavor fail.
        create("dev") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"$devBaseUrl\"")
            buildConfigField(
                "String",
                "STRIPE_PUBLISHABLE_KEY",
                "\"pk_test_51Tha0rHGu8AnfRoUiTqYX1Kt1QJ5bvN25ONTnckKYOeT53y8FrVWbXcyNdZfIL2U5FsuLP35OVTmqqO1Vwy0SzJm00I3w6Mn51\"",
            )
            buildConfigField("String", "AGORA_APP_ID", "\"483eaa4d11fb44b6acc91bf5ceae36c7\"")
        }
        // The backend has been in Stripe live mode since the 2026-10-17 cutover (backend 261017-tm2), so the pk_live
        // key below matches production.
        create("prod") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"$productionBaseUrl\"")
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
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
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
