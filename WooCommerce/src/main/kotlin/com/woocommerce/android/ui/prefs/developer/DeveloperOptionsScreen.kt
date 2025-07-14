package com.woocommerce.android.ui.prefs.developer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsViewModel.DeveloperOptionsViewState

@Composable
fun DeveloperOptionsScreen(viewModel: DeveloperOptionsViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        DeveloperOptionsScreen(it)
    }
}

@Composable
fun DeveloperOptionsScreen(viewState: DeveloperOptionsViewState) {
    LazyColumn {
        items(viewState.rows) { row ->
            when (row) {
                is DeveloperOptionsViewState.ListItem.ToggleableListItem -> {
                    ToggleableListItem(row)
                }

                is DeveloperOptionsViewState.ListItem.NonToggleableListItem -> {
                    NonToggleableListItem(row)
                }
            }
        }
    }
}

@Composable
private fun ToggleableListItem(
    item: DeveloperOptionsViewState.ListItem.ToggleableListItem,
    modifier: Modifier = Modifier
) {
    CommonItemLayout(item, modifier.clickable { item.onToggled(!item.isChecked) }) {
        Switch(
            checked = item.isChecked,
            onCheckedChange = item.onToggled,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
        )
    }
}

@Composable
private fun NonToggleableListItem(
    item: DeveloperOptionsViewState.ListItem.NonToggleableListItem,
    modifier: Modifier = Modifier
) {
    CommonItemLayout(item, modifier.clickable { item.onClick() }) {
        item.endIcon?.let {
            Icon(
                painter = painterResource(id = it),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun CommonItemLayout(
    item: DeveloperOptionsViewState.ListItem,
    modifier: Modifier = Modifier,
    additionalContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Image(
                painter = painterResource(item.icon),
                contentDescription = null,
                colorFilter = item.iconTint?.let { ColorFilter.tint(colorResource(id = it)) },
                modifier = Modifier
                    .size(72.dp)
            )

            Text(
                text = item.label.getText(),
                modifier = Modifier.weight(1f)
            )

            additionalContent?.invoke()
        }

        Divider()
    }
}
