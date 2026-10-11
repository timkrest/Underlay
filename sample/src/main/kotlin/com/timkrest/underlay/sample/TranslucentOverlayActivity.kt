// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
        enableEdgeToEdge()
        val initialBlurRadius = intent.getFloatExtra(EXTRA_BLUR_RADIUS, DEFAULT_BLUR_RADIUS.value)

        setContent {
            SampleTheme {
                var blurRadiusDp by rememberSaveable { mutableFloatStateOf(initialBlurRadius) }

                SideEffect { setResult(RESULT_OK, Intent().putExtra(EXTRA_BLUR_RADIUS, blurRadiusDp)) }
                TranslucentOverlay(
                    blurRadius = blurRadiusDp.dp,
                    onBlurRadiusChange = { radius -> blurRadiusDp = radius.value },
                    onClose = ::finish,
                )
            }
        }
    }

    object Contract : ActivityResultContract<Dp, Dp?>() {

        override fun createIntent(context: Context, input: Dp): Intent =
            Intent(context, TranslucentOverlayActivity::class.java).putExtra(EXTRA_BLUR_RADIUS, input.value)

        override fun parseResult(resultCode: Int, intent: Intent?): Dp? =
            intent?.takeIf { resultCode == Activity.RESULT_OK && it.hasExtra(EXTRA_BLUR_RADIUS) }
                ?.getFloatExtra(EXTRA_BLUR_RADIUS, DEFAULT_BLUR_RADIUS.value)
                ?.dp
    }

    private companion object {
        const val EXTRA_BLUR_RADIUS = "blurRadiusDp"
    }
}

@Composable
private fun TranslucentOverlay(blurRadius: Dp, onBlurRadiusChange: (Dp) -> Unit, onClose: () -> Unit) {
    val underlayState = rememberUnderlayState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = null, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        OverlayCard(
            kind = OverlayKind.TranslucentActivity,
            source = underlayState.source,
            blurRadius = blurRadius,
            onBlurRadiusChange = onBlurRadiusChange,
            onShuffle = { },
            onClose = onClose,
            modifier = Modifier
                .width(cardWidth())
                .sampleUnderlay(blurRadius = blurRadius, liveSnapshot = false, state = underlayState)
                .clickable(interactionSource = null, indication = null, onClick = {}),
        )
    }
}
