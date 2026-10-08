plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.kumpello.whereiseveryone.core"
    compileSdk = 37

    defaultConfig { minSdk = 28 }
    buildFeatures { compose = true }
    testFixtures { enable = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.bundles.compose.bom)
    api(libs.androidx.foundation.layout)
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.ktx)
    implementation(libs.compose.material.icons.extended)

    testFixturesApi(libs.test.junit)
    testFixturesApi(libs.coroutines.test)
    testImplementation(libs.test.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    debugImplementation(libs.compose.ui.tooling)
}
