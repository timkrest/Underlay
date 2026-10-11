// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.snapshot

import androidx.compose.ui.graphics.ImageBitmap

internal fun interface HostCapture {

    suspend fun capture(): ImageBitmap?
}
