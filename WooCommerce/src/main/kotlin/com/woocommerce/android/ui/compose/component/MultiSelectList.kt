package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun <T> MultiSelectList(
    items: List<T>,
    selectedItems: List<T>,
    onItemToggled: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemFormatter: T.() -> String = { toString() },
    itemKey: ((T) -> Any)? = null,
    allItemsButton: MultiSelectAllItemsButton? = null,
) {
    Column(modifier = modifier) {
        allItemsButton?.let {
            MultiSelectItem(
                item = it.text,
                isSelected = selectedItems.isEmpty(),
                onItemToggled = it.onClicked,
                modifier = Modifier.fillMaxWidth()
            )
            Divider()
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items, key = itemKey) { item ->
                MultiSelectItem(
                    item = itemFormatter(item),
                    isSelected = selectedItems.contains(item),
                    onItemToggled = { onItemToggled(item) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MultiSelectItem(
    item: String,
    isSelected: Boolean,
    onItemToggled: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = modifier
            .toggleable(
                value = isSelected,
                onValueChange = { onItemToggled() }
            )
            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            .heightIn(min = dimensionResource(id = R.dimen.major_300))
    ) {
        Text(
            text = item,
            modifier = Modifier
                .weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
fun rememberMultiSelectAllItemsButton(
    text: String,
    onClick: () -> Unit
): MultiSelectAllItemsButton {
    val currentOnClick by rememberUpdatedState(onClick)

    return remember(text) {
        MultiSelectAllItemsButton(
            text = text,
            onClicked = { currentOnClick() }
        )
    }
}

data class MultiSelectAllItemsButton(
    val text: String,
    val onClicked: () -> Unit
)

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun MultiSelectListPreview() {
    WooThemeWithBackground {
        val items by remember { mutableStateOf(List(20) { "Item $it" }) }
        var selectedItems by remember { mutableStateOf(emptyList<String>()) }
        val allItemsButton = rememberMultiSelectAllItemsButton(
            text = "All",
            onClick = { selectedItems = emptyList() }
        )

        MultiSelectList(
            items = items,
            selectedItems = selectedItems,
            onItemToggled = {
                selectedItems = if (selectedItems.contains(it)) selectedItems - it else selectedItems + it
            },
            allItemsButton = allItemsButton,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
        )
    }
}
