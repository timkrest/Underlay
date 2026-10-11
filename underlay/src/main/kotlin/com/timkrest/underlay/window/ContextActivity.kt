// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.window

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

internal fun Context.findActivity(): Activity? =
    generateSequence(this) { context -> (context as? ContextWrapper)?.baseContext }
        .firstOrNull { it is Activity } as? Activity
