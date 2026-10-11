// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.backdrop

internal interface HostWindowEvents {

    fun followDraws(isFollowing: Boolean)

    fun release()
}

internal typealias HostWindowEventsFactory = (onResize: () -> Unit, onDraw: () -> Unit) -> HostWindowEvents
