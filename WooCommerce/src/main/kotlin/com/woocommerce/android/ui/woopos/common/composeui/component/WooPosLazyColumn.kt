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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding

@Composable
fun WooPosLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    state: LazyListState = rememberLazyListState(),
    withBottomShadow: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    Box {
        LazyColumn(
            modifier = modifier,
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
            Shadow(modifier)
        }

        if (showBottomShadow.value && withBottomShadow) {
            Shadow(
                modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer(rotationZ = 180f)
            )
        }
    }
}

@Composable
private fun Shadow(modifier: Modifier) {
    WooPosCard(
        shape = MaterialTheme.shapes.large,
        backgroundColor = Color.Black.copy(alpha = 0.1f),
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp),
        elevation = 4.dp.toAdaptivePadding(),
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
                    elevation = 4.dp,
                ) {
                    Text(
                        "Item $i",
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth(),
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onSurface,
                    )
                }
            }
        }
    }
}
