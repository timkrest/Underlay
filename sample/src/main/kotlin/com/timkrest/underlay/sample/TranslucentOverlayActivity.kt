package com.timkrest.underlay.sample

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class TranslucentOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val blurRadius = intent.getFloatExtra(EXTRA_BLUR_RADIUS, DEFAULT_BLUR_RADIUS.value).dp

        setContent {
            SampleTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    UnderlayCard(
                        kind = OverlayKind.TranslucentActivity,
                        blurRadius = blurRadius,
                        onDismiss = ::finish,
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
