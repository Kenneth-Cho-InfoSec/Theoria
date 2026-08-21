/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01, kennethcho
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.search.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dot.gallery.R
import com.dot.gallery.core.Settings.Misc.rememberAllowBlur
import com.dot.gallery.feature_node.domain.model.LocationMedia
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.search.SearchMediaItem
import com.dot.gallery.ui.theme.BlackScrim
import com.dot.gallery.ui.theme.WhiterBlackScrim
import com.dot.gallery.ui.theme.isDarkTheme
import com.github.panpf.sketch.AsyncImage
import kotlinx.collections.immutable.ImmutableList

/**
 * A horizontal carousel of top locations, matching the LibraryScreen visual style.
 */
@Composable
fun LocationCarousel(
    locations: ImmutableList<LocationMedia>,
    onLocationClick: (LocationMedia) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = stringResource(R.string.locations),
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp)
) {
    if (locations.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(contentPadding)
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = locations,
                key = { it.location }
            ) { locationMedia ->
                val isDarkTheme = isDarkTheme()
                val allowBlur by rememberAllowBlur()
                val followTheme = remember(allowBlur) { !allowBlur }
                val gradientColor by animateColorAsState(
                    if (followTheme) {
                        if (isDarkTheme) BlackScrim else WhiterBlackScrim
                    } else BlackScrim,
                )
                Box(
                    modifier = Modifier
                        .width(164.dp)
                        .height(256.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onLocationClick(locationMedia) },
                ) {
                    AsyncImage(
                        uri = locationMedia.media.getUri().toString(),
                        contentScale = ContentScale.Crop,
                        contentDescription = locationMedia.location,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        gradientColor
                                    )
                                )
                            )
                            .padding(24.dp),
                        text = locationMedia.location,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

/**
 * A generic horizontal carousel for dynamic search items (MIME types, lens models, media modes, etc.).
 * Uses the same visual style as [LocationCarousel].
 */
@Composable
fun SearchCarousel(
    items: ImmutableList<SearchMediaItem>,
    onItemClick: (SearchMediaItem) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp)
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(contentPadding)
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = items,
                key = { "search_${it.key}" }
            ) { item ->
                val isDarkTheme = isDarkTheme()
                val allowBlur by rememberAllowBlur()
                val followTheme = remember(allowBlur) { !allowBlur }
                val gradientColor by animateColorAsState(
                    if (followTheme) {
                        if (isDarkTheme) BlackScrim else WhiterBlackScrim
                    } else BlackScrim,
                )
                Box(
                    modifier = Modifier
                        .width(164.dp)
                        .height(256.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onItemClick(item) },
                ) {
                    if (item.media != null) {
                        AsyncImage(
                            uri = item.media.getUri().toString(),
                            contentScale = ContentScale.Crop,
                            contentDescription = item.label,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        gradientColor
                                    )
                                )
                            )
                            .padding(24.dp),
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
