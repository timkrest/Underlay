// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.rememberUnderlayState

/** Returns the blur radius it was closed with as [EXTRA_BLUR_RADIUS] in its result. */
class TranslucentOverlayActivity : ComponentActivity() {

    private var blurRadiusDp by mutableFloatStateOf(DEFAULT_BLUR_RADIUS.value)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blurRadiusDp = savedInstanceState?.getFloat(EXTRA_BLUR_RADIUS)
            ?: intent.getFloatExtra(EXTRA_BLUR_RADIUS, DEFAULT_BLUR_RADIUS.value)

        setContent {
            SampleTheme {
                val underlayState = rememberUnderlayState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = null, indication = null, onClick = ::finish),
                    contentAlignment = Alignment.Center,
                ) {
                    OverlayCard(
                        kind = OverlayKind.TranslucentActivity,
                        source = underlayState.source,
                        blurRadius = blurRadiusDp.dp,
                        onBlurRadiusChange = { radius -> blurRadiusDp = radius.value },
                        onShuffle = { },
                        onClose = ::finish,
                        modifier = Modifier
                            .width(cardWidth())
                            .sampleUnderlay(blurRadius = blurRadiusDp.dp, liveSnapshot = false, state = underlayState)
                            .clickable(interactionSource = null, indication = null, onClick = {}),
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat(EXTRA_BLUR_RADIUS, blurRadiusDp)
    }

    override fun finish() {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_BLUR_RADIUS, blurRadiusDp))
        super.finish()
    }

    companion object {

        const val EXTRA_BLUR_RADIUS = "blurRadiusDp"

        fun intent(context: Context, blurRadius: Dp): Intent =
            Intent(context, TranslucentOverlayActivity::class.java)
                .putExtra(EXTRA_BLUR_RADIUS, blurRadius.value)
    }
}
