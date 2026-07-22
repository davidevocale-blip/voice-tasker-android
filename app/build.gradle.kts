plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.voicetasker.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.voicetasker.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.1.8"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase config from local.properties
        val localPropsFile = rootProject.file("local.properties")
        fun readProp(key: String): String = localPropsFile.takeIf { it.exists() }
            ?.readLines()
            ?.firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.trim() ?: ""

        buildConfigField("String", "SUPABASE_URL", "\"${readProp("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${readProp("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${readProp("GOOGLE_WEB_CLIENT_ID")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${readProp("GEMINI_API_KEY")}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("C:\\Users\\Davide\\Desktop\\voicetasker-release.jks")
            storePassword = "VoiceTasker2026!"
            keyAlias = "voicetasker"
            keyPassword = "VoiceTasker2026!"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Core
    implementation(libs.core.ktx)

    // Gemini AI (using OkHttp directly, no SDK needed)
    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.android)

    // Google Play Billing
    implementation(libs.billing.ktx)

    // Credential Manager (Google Sign-In)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    // Coil (user avatars)
    implementation(libs.coil.compose)

    // DataStore (session cache)
    implementation(libs.datastore.preferences)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
