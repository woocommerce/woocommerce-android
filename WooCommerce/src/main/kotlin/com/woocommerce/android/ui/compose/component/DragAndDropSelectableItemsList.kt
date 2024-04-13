package com.woocommerce.android.ui.compose.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.DraggableItem
import com.woocommerce.android.ui.compose.component.ShowDividers.All
import com.woocommerce.android.ui.compose.component.ShowDividers.Bottom
import com.woocommerce.android.ui.compose.component.ShowDividers.None
import com.woocommerce.android.ui.compose.dragContainerForDragHandle
import com.woocommerce.android.ui.compose.rememberDragDropState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> DragAndDropSelectableItemsList(
    items: List<T>,
    selectedItems: List<T>,
    onSelectionChange: (T, Boolean) -> Unit,
    onOrderChange: (fromIndex: Int, toIndex: Int) -> Unit,
    itemFormatter: T.() -> String = { toString() },
    itemKey: ((index: Int, item: T) -> Any),
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
            DraggableItem(dragDropState, i) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 4.dp else 1.dp, label = "card_elevation")
                val showDividers = when {
                    isDragging -> None
                    i == 0 -> All
                    else -> Bottom
                }
                DragAndDropItem(
                    showDividers = showDividers,
                    item = item,
                    title = itemFormatter(item),
                    isSelected = item in selectedItems,
                    onSelectionChange = onSelectionChange,
                    elevation = elevation
                ) {
                    Icon(
                        imageVector = Filled.DragHandle,
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
    }
}

enum class ShowDividers { Bottom, All, None }

@Composable
fun <T> DragAndDropItem(
    item: T,
    title: String,
    isSelected: Boolean,
    onSelectionChange: (T, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDividers: ShowDividers = ShowDividers.Bottom,
    elevation: Dp = 1.dp,
    isEnabled: Boolean = true,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        elevation = elevation,
        shape = RoundedCornerShape(0.dp)
    ) {
        val itemModifier = if (isEnabled) {
            Modifier
                .clickable { onSelectionChange(item, !isSelected) }
                .padding(16.dp)
        } else {
            Modifier
                .padding(16.dp)
        }
        Column {
            if (showDividers == All) Divider()
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
                    text = title,
                    modifier
                        .weight(2f)
                        .padding(horizontal = 16.dp)
                )
                dragHandle?.let { dragHandle -> dragHandle() }
            }
            if (showDividers != None) Divider()
        }
    }
}

@Composable
fun SelectionCheck(
    isSelected: Boolean,
    onSelectionChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val selectionDrawable = if (isSelected) {
        R.drawable.ic_rounded_chcekbox_checked
    } else {
        R.drawable.ic_rounded_chcekbox_unchecked
    }

    val colorFilter = if (isEnabled) null else ColorFilter.tint(Color.Gray)

    val description = stringResource(id = R.string.card_selection_control)
    val state = if (!isEnabled) stringResource(id = R.string.disabled) else ""

    val controlModifier = if (isEnabled && onSelectionChange != null) {
        modifier.clickable { onSelectionChange(!isSelected) }
    } else {
        modifier
    }

    Box(
        modifier = controlModifier
            .semantics {
                contentDescription = description
                stateDescription = state
            },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = selectionDrawable,
            modifier = modifier.wrapContentSize(),
            label = "itemSelection"
        ) { icon ->
            Image(
                painter = painterResource(id = icon),
                colorFilter = colorFilter,
                contentDescription = null
            )
        }
    }
}
