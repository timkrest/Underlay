package com.timkrest.underlay.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timkrest.underlay.rememberUnderlayState

private val BACKDROP_GUTTER = 12.dp

@Composable
internal fun SampleScreen(modifier: Modifier = Modifier) {
    var blurRadiusDp by rememberSaveable { mutableFloatStateOf(DEFAULT_BLUR_RADIUS.value) }
    var openKind by rememberSaveable { mutableStateOf<OverlayKind?>(null) }
    var backdropShift by rememberSaveable { mutableIntStateOf(0) }
    val underlayState = rememberUnderlayState()
    val blurRadius = blurRadiusDp.dp
    val tiles = rememberGradientTiles()
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                ControlsPanel(
                    blurRadius = blurRadius,
                    overlaySource = underlayState.source,
                    areTilesHardware = tiles?.areHardware,
                    onBlurRadiusChange = { blurRadiusDp = it.value },
                    onOpen = { kind ->
                        if (kind == OverlayKind.TranslucentActivity) {
                            context.startActivity(TranslucentOverlayActivity.intent(context, blurRadius))
                        } else {
                            openKind = kind
                        }
                    },
                )
            },
        ) { scaffoldPadding ->
            if (tiles != null) {
                Backdrop(
                    tiles = tiles,
                    contentPadding = scaffoldPadding.inflated(BACKDROP_GUTTER),
                    shift = backdropShift,
                )
            }
        }

        OverlayHost(
            kind = openKind,
            blurRadius = blurRadius,
            underlayState = underlayState,
            onRefresh = {
                backdropShift++
                underlayState.refresh()
            },
            onDismiss = { openKind = null },
        )
    }
}

@Composable
private fun PaddingValues.inflated(gutter: Dp): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection) + gutter,
        top = calculateTopPadding() + gutter,
        end = calculateEndPadding(layoutDirection) + gutter,
        bottom = calculateBottomPadding() + gutter,
    )
}

@Preview
@Composable
private fun SampleScreenPreview() {
    SampleTheme {
        SampleScreen()
    }
}
