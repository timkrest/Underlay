// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.backdrop

internal interface SystemBlurBehind {

    fun request(radiusPx: Int): Boolean

    fun withdraw()

    fun release()
}

internal typealias SystemBlurFactory = (onEnabledChange: () -> Unit) -> SystemBlurBehind
