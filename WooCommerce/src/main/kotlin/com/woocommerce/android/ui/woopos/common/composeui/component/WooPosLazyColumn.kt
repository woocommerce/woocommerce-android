package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    state: LazyListState = rememberLazyListState(),
    withBottomShadow: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    Box(modifier = modifier) {
        LazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            state = state,
            content = content
        )

        val showTopShadow = remember {
            derivedStateOf {
                state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
            }
        }

        val showBottomShadow = remember {
            derivedStateOf {
                val lastVisibleItem = state.layoutInfo.visibleItemsInfo.lastOrNull()
                val totalItemCount = state.layoutInfo.totalItemsCount

                if (lastVisibleItem != null) {
                    val lastItemPartiallyVisible =
                        lastVisibleItem.offset + lastVisibleItem.size > state.layoutInfo.viewportEndOffset
                    lastVisibleItem.index < totalItemCount - 1 || lastItemPartiallyVisible
                } else {
                    false
                }
            }
        }

        if (showTopShadow.value) {
            Shadow()
        }

        if (showBottomShadow.value && withBottomShadow) {
            Shadow(
                Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer(rotationZ = 180f)
            )
        }
    }
}

@Composable
private fun Shadow(modifier: Modifier = Modifier) {
    WooPosCard(
        shape = MaterialTheme.shapes.large,
        backgroundColor = Color.Black.copy(alpha = 0.1f),
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
        elevation = WooPosElevation.Medium,
    ) {}
}

@WooPosPreview
@Composable
fun WooPosLazyColumnPreview() {
    WooPosTheme {
        WooPosLazyColumn {
            items(10) { i ->
                WooPosCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = WooPosElevation.Medium,
                ) {
                    WooPosText(
                        "Item $i",
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth(),
                        style = WooPosTypography.BodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
