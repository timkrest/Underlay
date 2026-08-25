import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    applyUnderlayDefaults(libs)
    testOptions {
        targetSdk = libs.int("androidCompileSdk")
    }
}

extensions.configure<LibraryAndroidComponentsExtension> {
    finalizeDsl { android ->
        if (android.namespace == null) android.namespace = defaultNamespace(path)
    }
}

kotlin {
    jvmToolchain(libs.int("jvmToolchain"))
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.javaTarget.toString())
    }
    explicitApi()
}
