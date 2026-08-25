import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    applyUnderlayDefaults(libs)

    defaultConfig {
        targetSdk = libs.int("androidCompileSdk")
    }
}

extensions.configure<ApplicationAndroidComponentsExtension> {
    finalizeDsl { android ->
        if (android.namespace == null) android.namespace = defaultNamespace(path)
    }
}

kotlin {
    jvmToolchain(libs.int("jvmToolchain"))
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.javaTarget.toString())
    }
}
