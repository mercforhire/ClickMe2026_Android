import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

// Throwaway spike inputs live outside the repo (D-14). The file is optional: CI and fresh checkouts build with empty values.
val spikeInputs =
    Properties().apply {
        val inputsFile =
            layout.file(
                providers.provider {
                    File(providers.systemProperty("user.home").get(), ".clickme/spike-inputs.properties")
                },
            )
        val text = providers.fileContents(inputsFile).asText.orNull
        if (text != null) load(StringReader(text))
    }

fun spikeInput(key: String): String =
    "\"" +
        spikeInputs
            .getProperty(key, "")
            .trim()
            .replace("\\", "\\\\")
            .replace("\"", "\\\"") + "\""

android {
    namespace = "com.uptrendinvestments.clickmeapp.spike.stripe"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.uptrendinvestments.clickmeapp.spike.stripe"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "spike"

        // Dev flavor values copied from :app. Test-mode publishable key only, never the live key (D-12).
        buildConfigField("String", "BASE_URL", "\"https://clickme-backend-production.up.railway.app/api/v1\"")
        buildConfigField(
            "String",
            "STRIPE_PUBLISHABLE_KEY",
            "\"pk_test_51Tha0rHGu8AnfRoUiTqYX1Kt1QJ5bvN25ONTnckKYOeT53y8FrVWbXcyNdZfIL2U5FsuLP35OVTmqqO1Vwy0SzJm00I3w6Mn51\"",
        )
        buildConfigField("String", "CLIENT_EMAIL", spikeInput("spike.client.email"))
        buildConfigField("String", "EXPERT_ID", spikeInput("spike.stripe.expert_id"))
        buildConfigField("String", "TOPIC_ID", spikeInput("spike.stripe.topic_id"))
        buildConfigField("String", "SELECTED_DATE", spikeInput("spike.stripe.selected_date"))
        buildConfigField("String", "TIME_SLOT", spikeInput("spike.stripe.time_slot"))
        buildConfigField("String", "START_TIME", spikeInput("spike.stripe.start_time"))
        buildConfigField("String", "DURATION_MINUTES", spikeInput("spike.stripe.duration_minutes"))
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.stripe.paymentsheet)
}
