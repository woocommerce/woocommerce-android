package com.woocommerce.android.ui.compose.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.DragDropState
import com.woocommerce.android.ui.compose.DraggableItem
import com.woocommerce.android.ui.compose.dragContainerForDragHandle
import com.woocommerce.android.ui.compose.rememberDragDropState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> DragAndDropItemsList(
    items: List<T>,
    onOrderChange: (fromIndex: Int, toIndex: Int) -> Unit,
    itemKey: ((index: Int, item: T) -> Any),
    modifier: Modifier = Modifier,
    isItemDraggable: (T) -> Boolean = { true },
    itemContent: @Composable (item: T, dragDropState: DragDropState) -> Unit
) {
    val listState = rememberLazyListState()
    // This is needed to make sure that we access the updated list in the captured value in isDraggable lambda
    val itemsState by rememberUpdatedState(newValue = items)
    val dragDropState = rememberDragDropState(
        listState,
        isDraggable = { index -> isItemDraggable(itemsState[index]) }
    ) { fromIndex, toIndex -> onOrderChange(fromIndex, toIndex) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .background(MaterialTheme.colors.surface)
    ) {
        itemsIndexed(items = itemsState, key = itemKey) { i, item ->
            DraggableItem(dragDropState, i) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 1.dp, label = "card_elevation")
                val showDividers = when {
                    isDragging -> ShowDividers.None
                    i == 0 -> ShowDividers.All
                    else -> ShowDividers.Bottom
                }
                Card(
                    modifier = modifier,
                    elevation = elevation,
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column {
                        if (showDividers == ShowDividers.All) Divider()
                        itemContent(item, dragDropState)
                        if (showDividers != ShowDividers.None) Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun <T> DragAndDropSelectableItemsList(
    items: List<T>,
    selectedItems: List<T>,
    onSelectionChange: (T, Boolean) -> Unit,
    onOrderChange: (fromIndex: Int, toIndex: Int) -> Unit,
    itemKey: ((index: Int, item: T) -> Any),
    modifier: Modifier = Modifier,
    itemFormatter: @Composable T.() -> String = { toString() }
) {
    DragAndDropItemsList(
        items = items,
        onOrderChange = onOrderChange,
        itemKey = itemKey,
        modifier
    ) { item, dragDropState ->
        DragAndDropSelectableItem(
            item = item,
            isSelected = item in selectedItems,
            dragDropState = dragDropState,
            onSelectionChange = onSelectionChange,
            itemKey = { itemKey(items.indexOf(item), item) },
            itemFormatter = itemFormatter
        )
    }
}

@Composable
fun <T> DragAndDropSelectableItem(
    item: T,
    isSelected: Boolean,
    dragDropState: DragDropState,
    onSelectionChange: (T, Boolean) -> Unit,
    itemKey: (item: T) -> Any,
    modifier: Modifier = Modifier,
    itemFormatter: @Composable T.() -> String = { toString() },
    isEnabled: Boolean = true,
) {
    val itemModifier = if (isEnabled) {
        Modifier
            .clickable { onSelectionChange(item, !isSelected) }
            .padding(16.dp)
    } else {
        Modifier
            .padding(16.dp)
    }
    Row(
        modifier = itemModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionCheck(
            isSelected = isSelected,
            onSelectionChange = null,
            isEnabled = isEnabled
        )
        Text(
            text = itemFormatter(item),
            modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        Icon(
            imageVector = Filled.DragHandle,
            contentDescription = stringResource(id = R.string.drag_handle),
            modifier = Modifier
                .dragContainerForDragHandle(
                    dragDropState = dragDropState,
                    key = itemKey(item),
                )
                .clickable(
                    onClick = {},
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        )
    }
}

enum class ShowDividers { Bottom, All, None }
