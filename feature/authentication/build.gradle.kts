plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.kumpello.whereiseveryone.feature.authentication"
    compileSdk = 37

    defaultConfig { minSdk = 28 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.bom)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.bundles.viewmodel)
    implementation(libs.bundles.runtime)
    implementation(libs.navigation)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)
    implementation(libs.timber)

    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    debugImplementation(libs.compose.ui.tooling)
}
