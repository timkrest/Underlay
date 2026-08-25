plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.mavenPublish.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
}
