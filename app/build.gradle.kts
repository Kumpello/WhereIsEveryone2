plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.ksp.get().pluginId) version (libs.plugins.ksp.get().version.toString())
    id(libs.plugins.kotlin.parcelize.get().pluginId)
}

android {
    namespace = "com.kumpello.whereiseveryone"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.kumpello.whereiseveryone"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("production") {
            dimension = "version"
            buildConfigField("String", "BASE_URL", "\"https://release.com/\"")
        }
        create("development") {
            dimension = "version"
            applicationIdSuffix = ".development"
            versionNameSuffix = "-development"
            buildConfigField("String", "BASE_URL", "\"https://test.com/\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
    kotlinOptions {
        jvmTarget = "19"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(platform(libs.kotlin.bom))
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.bom)
    implementation(libs.bundles.viewmodel)
    implementation(libs.bundles.retrofit)
    implementation(libs.bundles.moshi)
    implementation(libs.logging.interceptor)
    implementation(libs.timber)
    implementation(libs.compose.destinations.core)
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.koin)
    implementation(libs.koin.compose)
    //implementation(libs.koin.annotations)
    //implementation(libs.koin.ksp)
    implementation(libs.play.services.location)
    implementation(libs.androidx.security.crypto)

    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.compose.destinations.ksp)

    testImplementation(libs.test.junit)

    androidTestImplementation(libs.android.test.junit)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
