import java.util.Properties
import java.io.FileInputStream

plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.ksp.get().pluginId) version (libs.plugins.ksp.get().version.toString())
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.oss.licenses)
}

// --- Signing config resolution: env vars (CI/CD) win, keystore.properties (local) is the fallback ---
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

fun signingProp(propKey: String, envKey: String): String? =
    System.getenv(envKey) ?: keystoreProperties.getProperty(propKey)

android {
    namespace = "com.kumpello.whereiseveryone"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    defaultConfig {
        applicationId = "com.kumpello.whereiseveryone"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.8"

        val mapboxToken: String = project.findProperty("MAPBOX_TOKEN") as? String ?: ""
        resValue("string", "mapbox_access_token", mapboxToken)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // storeFile path: KEYSTORE_FILE env var, or "storeFile" in keystore.properties,
            // or defaults to release.keystore sitting next to this build file
            storeFile = file(signingProp("storeFile", "KEYSTORE_FILE") ?: "release.keystore")
            storePassword = signingProp("storePassword", "KEYSTORE_PASSWORD")
            keyAlias = signingProp("keyAlias", "KEY_ALIAS")
            keyPassword = signingProp("keyPassword", "KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Optional: makes plain "Run" install a release-signed APK too.
            // Remove this block if you want Run to keep using the debug keystore.
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("production") {
            dimension = "version"
            buildConfigField("String", "BASE_URL", "\"https://api.where-is-everyone.com/\"")
            buildConfigField("Boolean", "IS_PREMIUM", "false")
        }
        create("productionPremium") {
            dimension = "version"
            applicationIdSuffix = ".premium"
            buildConfigField("String", "BASE_URL", "\"https://api.where-is-everyone.com/\"")
            buildConfigField("Boolean", "IS_PREMIUM", "true")
        }
        create("development") {
            dimension = "version"
            applicationIdSuffix = ".development"
            versionNameSuffix = "-development"
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.216:8080/\"")
            buildConfigField("Boolean", "IS_PREMIUM", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(platform(libs.kotlin.bom))
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.bom)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.bundles.viewmodel)
    implementation(libs.bundles.runtime)
    implementation(libs.bundles.koin)
    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.retrofit)
    implementation(libs.bundles.moshi)
    implementation(libs.logging.interceptor)
    implementation(libs.timber)
    implementation(libs.navigation)
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.converter.moshi)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.oss.licenses)

    implementation(libs.play.services.location)
    implementation(libs.play.services.appset)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.code.scanner)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore)
    implementation(libs.tink.android)
    implementation(libs.mapbox)
    implementation(libs.mapbox.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.zxing.core)
    ksp(libs.room.compiler)

    ksp(libs.moshi.kotlin.codegen)
    testImplementation(libs.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.android.test.junit)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}