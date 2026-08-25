package com.timkrest.underlay.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val TILE_REPEATS = 4
private const val BANNER_EVERY = 7
private const val SQUARE_RATIO = 1f
private const val BANNER_RATIO = 2.1f

@Composable
internal fun Backdrop(tiles: GradientTiles, contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = tiles.images.size * TILE_REPEATS,
            span = { index -> GridItemSpan(if (index.isBanner()) maxLineSpan else 1) },
        ) { index ->
            BackdropTile(index = index, image = tiles.images[index % tiles.images.size])
        }
    }
}

@Composable
private fun BackdropTile(index: Int, image: ImageBitmap) {
    Box(
        modifier = Modifier
            .aspectRatio(if (index.isBanner()) BANNER_RATIO else SQUARE_RATIO)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.55f),
                    ),
                ),
        )
        Text(
            text = "Frame ${index + 1}",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        )
    }
}

private fun Int.isBanner(): Boolean = this % BANNER_EVERY == BANNER_EVERY - 1

@Preview(heightDp = 420)
@Composable
private fun BackdropPreview() {
    SampleTheme {
        Backdrop(
            tiles = rememberGradientTiles(),
            contentPadding = PaddingValues(12.dp),
        )
    }
}
