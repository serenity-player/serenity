import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing comes from environment variables (CI) or an untracked keystore.properties (local).
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(env: String, prop: String): String? = System.getenv(env) ?: keystoreProps.getProperty(prop)

android {
    namespace = "io.github.serenity_player.serenity"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.serenity_player.serenity"
        minSdk = 31 // Material You dynamic color everywhere
        targetSdk = 35
        // CI passes -PversionName=<tag without v> and -PversionCode=<run number>.
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.0"
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("SERENITY_KEYSTORE_FILE", "storeFile")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = signingValue("SERENITY_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("SERENITY_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("SERENITY_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Left off on purpose: shrinking needs keep-rules for Retrofit + kotlinx.serialization that
            // should be verified on a device first. Turning it on later is a straightforward follow-up.
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.browser)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.androidx.palette)

    testImplementation(libs.junit)
}
