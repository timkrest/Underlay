// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}
