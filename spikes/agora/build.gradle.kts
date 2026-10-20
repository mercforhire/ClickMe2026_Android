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
    namespace = "com.uptrendinvestments.clickmeapp.spike.agora"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.uptrendinvestments.clickmeapp.spike.agora"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "spike"

        // Dev flavor values copied from :app.
        buildConfigField("String", "BASE_URL", "\"https://clickme-backend-production.up.railway.app/api/v1\"")
        buildConfigField("String", "AGORA_APP_ID", "\"483eaa4d11fb44b6acc91bf5ceae36c7\"")
        buildConfigField("String", "CLIENT_EMAIL", spikeInput("spike.client.email"))
        buildConfigField("String", "BOOKING_ID", spikeInput("spike.agora.booking_id"))
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.agora.voice.sdk)
}
