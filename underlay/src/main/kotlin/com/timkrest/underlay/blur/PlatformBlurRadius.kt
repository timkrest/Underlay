// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.blur

private const val SIGMA_PER_RADIUS = 0.57735f
private const val SIGMA_OFFSET = 0.5f

internal fun sigmaForPlatformRadius(radius: Float): Float = if (radius > 0f) SIGMA_PER_RADIUS * radius + SIGMA_OFFSET else 0f

internal fun platformRadiusForSigma(sigma: Float): Float = ((sigma - SIGMA_OFFSET) / SIGMA_PER_RADIUS).coerceAtLeast(0f)
