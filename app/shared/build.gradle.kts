import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import io.github.frankois944.spmForKmp.swiftPackageConfig
import java.net.URI

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.spmForKmp)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.swiftPackageConfig {
            dependency {
                remotePackageVersion(
                    url = URI("https://github.com/maplibre/maplibre-gl-native-distribution.git"),
                    products = { add("MapLibre", exportToKotlin = true) },
                    packageName = "maplibre-gl-native-distribution",
                    version = "6.25.1",
                )
            }
        }

        val mapLibreVariant = when (iosTarget.name) {
            "iosArm64" -> "arm64-apple-ios"
            "iosSimulatorArm64" -> "arm64-apple-ios-simulator"
            else -> error("Unsupported iOS target: ${iosTarget.name}")
        }
        val mapLibreFrameworkPath =
            "${layout.buildDirectory.get()}/spmKmpPlugin/${iosTarget.name}/scratch/$mapLibreVariant/release/"
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
        iosTarget.binaries.all {
            linkerOpts("-F$mapLibreFrameworkPath", "-rpath", mapLibreFrameworkPath)
        }
    }

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    androidLibrary {
        namespace = "com.naslabs.yardscape.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        val maplibreMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.maplibre.compose)
            }
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
        androidMain.get().dependsOn(maplibreMain)
        commonMain.dependencies {
            api(projects.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.ktor.clientCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.clientMock)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
        jsMain.get().dependsOn(maplibreMain)
        iosMain.get().dependsOn(maplibreMain)
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
