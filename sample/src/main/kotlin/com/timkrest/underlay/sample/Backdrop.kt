// Copyright 2026 Timofey Krestyanov
// SPDX-License-Identifier: Apache-2.0
package com.timkrest.underlay.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

internal val FEED_GUTTER = 10.dp

private const val CARD_COUNT = 36
private const val HERO_EVERY = 9
private val FEED_CARD_SHAPE = RoundedCornerShape(22.dp)
private val CARD_RATIOS = listOf(1f, 0.78f, 1.25f, 0.9f, 1.1f)
private val SCRIM = Brush.verticalGradient(
    0.45f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.7f),
)
private val AVATAR_FILL = Color.White.copy(alpha = 0.22f)
private val AVATAR_BORDER = Color.White.copy(alpha = 0.6f)
private val SUBTITLE = Color.White.copy(alpha = 0.78f)

private val CAPTIONS = listOf(
    "Aurora over Tromsø" to "Night · 12 min ago",
    "Dunes at dusk" to "Golden hour · 1 h ago",
    "Neon alley" to "Street · 3 h ago",
    "Coral reef" to "Underwater · yesterday",
    "Citrus still life" to "Studio · 2 d ago",
    "Glacier lagoon" to "Travel · 2 d ago",
    "Sunset surf" to "Sport · 3 d ago",
    "Lavender fields" to "Nature · 4 d ago",
    "Night market" to "Food · 5 d ago",
    "Cherry blossoms" to "Spring · 1 w ago",
    "Lilac hour" to "Portrait · 1 w ago",
    "Mint morning" to "Lifestyle · 2 w ago",
)

@Composable
internal fun Backdrop(
    tiles: GradientTiles,
    contentPadding: PaddingValues,
    heroRatio: Float,
    modifier: Modifier = Modifier,
    shift: Int = 0,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(150.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(FEED_GUTTER),
        verticalItemSpacing = FEED_GUTTER,
    ) {
        item(span = StaggeredGridItemSpan.FullLine) { FeedHeader() }
        items(
            count = CARD_COUNT,
            span = { index -> if (index.isHero()) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane },
        ) { index ->
            FeedCard(index = index, image = tiles.images[(index + shift) % tiles.images.size], heroRatio = heroRatio)
        }
    }
}

@Composable
private fun FeedHeader() {
    Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
        Text(
            text = "Underlay",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Open an overlay and watch the feed frost behind it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeedCard(index: Int, image: ImageBitmap, heroRatio: Float) {
    val (title, subtitle) = CAPTIONS[index % CAPTIONS.size]

    Box(
        modifier = Modifier
            .aspectRatio(if (index.isHero()) heroRatio else CARD_RATIOS[index % CARD_RATIOS.size])
            .clip(FEED_CARD_SHAPE),
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
                .background(SCRIM),
        )
        Avatar(
            letter = title.first(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                style = if (index.isHero()) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = subtitle, color = SUBTITLE, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Avatar(letter: Char, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(AVATAR_FILL)
            .border(1.dp, AVATAR_BORDER, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = letter.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

private fun Int.isHero(): Boolean = this % HERO_EVERY == 0

@Preview(heightDp = 520)
@Composable
private fun BackdropPreview() {
    SampleTheme {
        Backdrop(
            tiles = remember { gradientTiles() },
            contentPadding = PaddingValues(FEED_GUTTER),
            heroRatio = 1.8f,
        )
    }
}
