package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding

@Composable
fun WooPosLazyColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit
) {
    Box {
        LazyColumn(
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            state = state,
            content = content
        )

        val showShadow = remember {
            derivedStateOf {
                state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
            }
        }

        if (showShadow.value) {
            val height = 16.dp.toAdaptivePadding()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colors.onSurface.copy(alpha = .03f),
                                    MaterialTheme.colors.onSurface.copy(alpha = .03f),
                                    MaterialTheme.colors.onSurface.copy(alpha = .03f),
                                    MaterialTheme.colors.onSurface.copy(alpha = .03f),
                                    Color.Transparent,
                                ),
                                startX = 0f,
                                endX = Float.POSITIVE_INFINITY
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colors.onSurface.copy(alpha = .1f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY = height.value
                            )
                        )
                )
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosLazyColumnPreview() {
    WooPosTheme
    WooPosLazyColumn {
        items(10) { i ->
            Card(
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
