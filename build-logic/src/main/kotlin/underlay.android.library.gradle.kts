// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    applyUnderlayDefaults(libs)
    testOptions {
        targetSdk = libs.int("androidTargetSdk")
    }
}

kotlin {
    applyUnderlayDefaults(libs)
    explicitApi()
}
