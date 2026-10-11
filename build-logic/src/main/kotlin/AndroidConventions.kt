// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.int(alias: String): Int = version(alias).toInt()

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

internal val VersionCatalog.javaTarget: JavaVersion get() = JavaVersion.toVersion(version("jvmTarget"))

internal fun CommonExtension<*, *, *, *, *, *>.applyUnderlayDefaults(libs: VersionCatalog) {
    compileSdk = libs.int("androidCompileSdk")

    defaultConfig {
        minSdk = libs.int("androidMinSdk")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = libs.javaTarget
        targetCompatibility = libs.javaTarget
    }

    buildFeatures {
        buildConfig = false
        resValues = false
        shaders = false
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

internal fun KotlinAndroidProjectExtension.applyUnderlayDefaults(libs: VersionCatalog) {
    jvmToolchain(libs.int("jvmToolchain"))
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.javaTarget.toString()))
    }
}
