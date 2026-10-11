plugins {
    alias(libs.plugins.underlay.android.library)
    alias(libs.plugins.underlay.publish)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.timkrest.underlay"

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.foundation)
    androidTestImplementation(libs.androidx.compose.material3)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // Compose ui-test resolves Espresso 3.5, which reflects on APIs Android 16 removed.
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
