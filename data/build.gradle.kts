plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.kumpello.whereiseveryone.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_19
        targetCompatibility = JavaVersion.VERSION_19
    }
}

dependencies {
    api(libs.retrofit2)
    implementation(libs.room.runtime)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.retrofit2.converter.moshi)
    implementation(libs.bundles.moshi)
    implementation(libs.logging.interceptor)
    implementation(libs.room.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore)
    implementation(libs.tink.android)
    implementation(libs.play.services.appset)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin)
    implementation(libs.timber)
    ksp(libs.room.compiler)
    ksp(libs.moshi.kotlin.codegen)

    testImplementation(testFixtures(project(":core")))
    testImplementation(libs.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.android.test.junit)
    androidTestImplementation(libs.coroutines.test)
}
