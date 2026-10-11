// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.rememberUnderlayState

class TranslucentOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialBlurRadius = intent.getFloatExtra(EXTRA_BLUR_RADIUS, DEFAULT_BLUR_RADIUS.value).dp

        setContent {
            var blurRadiusDp by rememberSaveable { mutableFloatStateOf(initialBlurRadius.value) }

            SampleTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    UnderlayCard(
                        kind = OverlayKind.TranslucentActivity,
                        blurRadius = blurRadiusDp.dp,
                        underlayState = rememberUnderlayState(),
                        onDismiss = ::finish,
                        onRefresh = {},
                        onBlurRadiusChange = { radius -> blurRadiusDp = radius.value },
                        modifier = Modifier.width(CARD_WIDTH),
                    )
                }
            }
        }
    }

    companion object {

        private const val EXTRA_BLUR_RADIUS = "blurRadiusDp"

        fun intent(context: Context, blurRadius: Dp): Intent =
            Intent(context, TranslucentOverlayActivity::class.java)
                .putExtra(EXTRA_BLUR_RADIUS, blurRadius.value)
    }
}
