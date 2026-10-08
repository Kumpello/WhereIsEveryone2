import java.util.Properties
import java.io.FileInputStream
import java.io.ByteArrayOutputStream
import com.android.build.api.artifact.SingleArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.ksp.get().pluginId) version (libs.plugins.ksp.get().version.toString())
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.oss.licenses)
}

configurations.all {
    resolutionStrategy {
        force("androidx.concurrent:concurrent-futures:1.3.0")
    }
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
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
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

@DisableCachingByDefault(because = "Installs and launches an app on a connected device")
abstract class RunAndroidVariantTask : DefaultTask() {
    @get:InputDirectory
    abstract val apkDirectory: DirectoryProperty

    @get:Input
    abstract val applicationId: Property<String>

    @get:Internal
    abstract val adbExecutable: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val deviceSerial: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun installAndLaunch() {
        val adb = adbExecutable.get().asFile.absolutePath
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(adb, "devices")
            standardOutput = output
        }.assertNormalExitValue()

        val devices = output.toString(Charsets.UTF_8.name()).lineSequence()
            .map { it.trim().split(Regex("\\s+")) }
            .filter { it.size == 2 && it[1] == "device" }
            .map { it[0] }
            .toList()
        val requestedSerial = deviceSerial.orNull?.takeIf { it.isNotBlank() }
        val serial = if (requestedSerial != null) {
            if (requestedSerial !in devices) {
                throw GradleException("The selected Android device is not connected and authorized.")
            }
            requestedSerial
        } else {
            devices.singleOrNull() ?: throw GradleException(
                "Connect one authorized Android device, or select one with -PandroidRunSerial=SERIAL or ANDROID_SERIAL."
            )
        }
        val apk = apkDirectory.get().asFile.listFiles { file -> file.extension == "apk" }
            ?.singleOrNull() ?: throw GradleException("Expected one APK for this variant.")

        execOperations.exec {
            commandLine(adb, "-s", serial, "install", "-r", apk.absolutePath)
        }.assertNormalExitValue()
        execOperations.exec {
            commandLine(
                adb, "-s", serial, "shell", "am", "start", "-W",
                "-a", "android.intent.action.MAIN",
                "-c", "android.intent.category.LAUNCHER",
                "-p", applicationId.get()
            )
        }.assertNormalExitValue()
    }
}

androidComponents.onVariants { variant ->
    val variantTaskSuffix = variant.name.replaceFirstChar { it.uppercaseChar() }
    tasks.register<RunAndroidVariantTask>("run$variantTaskSuffix") {
        group = "application"
        description = "Builds, installs, and launches ${variant.name} on one Android device."
        dependsOn("assemble$variantTaskSuffix")
        apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
        applicationId.set(variant.applicationId)
        adbExecutable.set(androidComponents.sdkComponents.adb)
        deviceSerial.set(providers.gradleProperty("androidRunSerial").orElse(providers.environmentVariable("ANDROID_SERIAL")))
    }
}
