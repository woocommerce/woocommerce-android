package com.woocommerce.android.ui.analytics.hub.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.DraggableItem
import com.woocommerce.android.ui.compose.component.DragAndDropItem
import com.woocommerce.android.ui.compose.component.ShowDividers
import com.woocommerce.android.ui.compose.dragContainerForDragHandle
import com.woocommerce.android.ui.compose.rememberDragDropState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HubSettingsCardItemsList(
    items: List<AnalyticCardConfigurationUI>,
    selectedItems: List<AnalyticCardConfigurationUI>,
    onSelectionChange: (AnalyticCardConfigurationUI, Boolean) -> Unit,
    onOrderChange: (fromIndex: Int, toIndex: Int) -> Unit,
    onExplorePlugin: (url: String) -> Unit,
    itemFormatter: AnalyticCardConfigurationUI.() -> String = { toString() },
    itemKey: ((index: Int, item: AnalyticCardConfigurationUI) -> Any),
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex -> onOrderChange(fromIndex, toIndex) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        itemsIndexed(items = items, key = itemKey) { i, item ->
            when (item) {
                is AnalyticCardConfigurationUI.SelectableCardConfigurationUI -> {
                    DraggableItem(dragDropState, i) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 4.dp else 1.dp, label = "card_elevation")
                        val showDividers = when {
                            isDragging -> ShowDividers.None
                            i == 0 -> ShowDividers.All
                            else -> ShowDividers.Bottom
                        }
                        DragAndDropItem(
                            showDividers = showDividers,
                            item = item,
                            title = itemFormatter(item),
                            isSelected = item in selectedItems,
                            onSelectionChange = onSelectionChange,
                            elevation = elevation,
                            isEnabled = item.isEnabled
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DragHandle,
                                contentDescription = stringResource(id = R.string.drag_handle),
                                modifier = Modifier
                                    .dragContainerForDragHandle(
                                        dragDropState = dragDropState,
                                        key = itemKey(i, item),
                                    )
                                    .clickable(
                                        onClick = {},
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                            )
                        }
                    }
                }

                is AnalyticCardConfigurationUI.ExploreCardConfigurationUI -> {
                    ExplorePluginItem(
                        item = item,
                        onExplorePlugin = onExplorePlugin,
                        pos = i
                    )
                }
            }
        }
    }
}

@Composable
fun ExplorePluginItem(
    item: AnalyticCardConfigurationUI.ExploreCardConfigurationUI,
    onExplorePlugin: (url: String) -> Unit,
    pos: Int,
    modifier: Modifier = Modifier
) {
    Card(
        elevation = 2.dp,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            if (pos == 0) Divider()
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable { onExplorePlugin(item.url) }
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    item.title,
                    Modifier
                        .padding(start = 40.dp)
                        .align(Alignment.CenterStart)
                )
                Card(
                    elevation = 1.dp,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 8.dp)
                ) {
                    Box(

                        modifier = Modifier
                            .background(MaterialTheme.colors.primary)
                            .padding(vertical = 6.dp, horizontal = 12.dp)
                    ) {
                        Text("Explore!", color = MaterialTheme.colors.onPrimary)
                    }
                }
            }
            Divider()
        }
    }
}
