import org.gradle.api.artifacts.component.ModuleComponentSelector

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.oss.licenses) apply false
    alias(libs.plugins.koin.plugins) apply false
}

// Mapbox's Vulkan preview has no OpenGL fallback; keep it an explicit test build option.
val useMapboxVulkan = providers.gradleProperty("mapboxVulkan")
    .map { it.toBooleanStrict() }
    .orElse(false)
    .get()

if (useMapboxVulkan) {
    subprojects {
        configurations.configureEach {
            resolutionStrategy.dependencySubstitution {
                all {
                    val dependency = requested as? ModuleComponentSelector
                    if (dependency?.group == "com.mapbox.maps" &&
                        dependency.module in setOf("android-core", "android-core-ndk27")
                    ) {
                        val vulkanModule = dependency.module.replace("android-core", "android-core-vulkan")
                        useTarget(
                            "com.mapbox.maps:$vulkanModule:${dependency.version}",
                            "Explicitly enabled Mapbox Vulkan preview",
                        )
                    }
                }
            }
        }
    }
}
