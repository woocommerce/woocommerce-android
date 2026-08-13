package com.woocommerce.android.ui.filters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.AlertDialog
import com.woocommerce.android.ui.compose.component.SelectionCheck
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.filters.FilterHistoryViewModel.ViewState

@Composable
fun FilterHistoryScreen(viewModel: FilterHistoryViewModel) {
    val viewState by viewModel.viewState.collectAsState()
    FilterHistoryScreen(
        viewState = viewState,
        onFilterClick = viewModel::onFilterSelected,
        onApplyClick = viewModel::onApplyClicked,
        onCancelClick = viewModel::onCancelClicked,
        onDeleteFilter = viewModel::onDeleteFilter,
        onClearHistoryClick = viewModel::onClearHistoryClicked,
        onClearHistoryConfirm = viewModel::onClearHistoryConfirmed,
        onClearHistoryDismiss = viewModel::onClearHistoryDismissed
    )
}

@Composable
fun FilterHistoryScreen(
    viewState: ViewState,
    onFilterClick: (SavedFilter) -> Unit,
    onApplyClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDeleteFilter: (SavedFilter) -> Unit,
    onClearHistoryClick: () -> Unit,
    onClearHistoryConfirm: () -> Unit,
    onClearHistoryDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.filter_history_title),
                navigationIcon = ImageVector.vectorResource(id = R.drawable.ic_gridicons_cross_24dp),
                navigationIconContentDescription = stringResource(id = R.string.filter_history_cancel),
                onNavigationButtonClick = onCancelClick,
                actions = {
                    WCTextButton(
                        onClick = onApplyClick,
                        enabled = viewState.isApplyEnabled,
                        text = stringResource(id = R.string.filter_history_apply)
                    )
                }
            )
        }
    ) { paddingValues ->
        if (viewState.isEmpty) {
            FilterHistoryEmptyState(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )
        } else {
            FilterHistoryList(
                viewState = viewState,
                onFilterClick = onFilterClick,
                onDeleteFilter = onDeleteFilter,
                onClearHistoryClick = onClearHistoryClick,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )
        }
    }

    if (viewState.showClearHistoryConfirmation) {
        ClearHistoryConfirmationDialog(
            onConfirm = onClearHistoryConfirm,
            onDismiss = onClearHistoryDismiss
        )
    }
}

@Composable
private fun FilterHistoryList(
    viewState: ViewState,
    onFilterClick: (SavedFilter) -> Unit,
    onDeleteFilter: (SavedFilter) -> Unit,
    onClearHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text(
                    text = stringResource(id = R.string.filter_history_recent_header).uppercase(),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(id = R.dimen.major_100),
                        vertical = dimensionResource(id = R.dimen.minor_100)
                    )
                )
            }
            items(items = viewState.filters, key = { it.id }) { filter ->
                SwipeableFilterHistoryRow(
                    filter = filter,
                    isSelected = filter.id == viewState.selectedFilter?.id,
                    onClick = { onFilterClick(filter) },
                    onDelete = { onDeleteFilter(filter) }
                )
                Divider(modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100)))
            }
        }
        Divider()
        WCTextButton(
            onClick = onClearHistoryClick,
            text = stringResource(id = R.string.filter_history_clear),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.minor_100))
        )
    }
}

@Composable
private fun SwipeableFilterHistoryRow(
    filter: SavedFilter,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (swipeToDismissBoxState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_delete_filled_24dp),
                    contentDescription = stringResource(id = R.string.filter_history_delete),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorResource(id = R.color.woo_red_50))
                        .wrapContentSize(Alignment.CenterEnd)
                        .padding(end = dimensionResource(id = R.dimen.major_100)),
                    tint = colorResource(id = R.color.woo_white)
                )
            }
        },
        onDismiss = {
            if (it == SwipeToDismissBoxValue.EndToStart) onDelete()
            it != SwipeToDismissBoxValue.EndToStart
        }
    ) {
        FilterHistoryRow(
            filter = filter,
            isSelected = isSelected,
            onClick = onClick
        )
    }
}

@Composable
private fun FilterHistoryRow(
    filter: SavedFilter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .clickable { onClick() }
            .padding(dimensionResource(id = R.dimen.major_100)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = filter.readableString,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.weight(1f)
        )
        SelectionCheck(
            isSelected = isSelected,
            onSelectionChange = { onClick() }
        )
    }
}

@Composable
private fun FilterHistoryEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(dimensionResource(id = R.dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_history_24dp),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(dimensionResource(id = R.dimen.image_major_64))
        )
        Text(
            text = stringResource(id = R.string.filter_history_empty),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
        )
    }
}

@Composable
private fun ClearHistoryConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text = stringResource(id = R.string.filter_history_clear_confirmation)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.filter_history_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.filter_history_cancel))
            }
        },
        neutralButton = {}
    )
}

@LightDarkThemePreviews
@Composable
private fun FilterHistoryScreenPreview() {
    WooThemeWithBackground {
        FilterHistoryScreen(
            viewState = ViewState(
                filters = listOf(
                    SavedFilter(id = 1, readableString = "Processing, Last 30 days", payload = ""),
                    SavedFilter(id = 2, readableString = "Completed", payload = ""),
                    SavedFilter(id = 3, readableString = "Cancelled, John Doe", payload = "")
                ),
                selectedFilter = SavedFilter(id = 2, readableString = "Completed", payload = "")
            ),
            onFilterClick = {},
            onApplyClick = {},
            onCancelClick = {},
            onDeleteFilter = {},
            onClearHistoryClick = {},
            onClearHistoryConfirm = {},
            onClearHistoryDismiss = {}
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun FilterHistoryEmptyPreview() {
    WooThemeWithBackground {
        FilterHistoryScreen(
            viewState = ViewState(filters = emptyList()),
            onFilterClick = {},
            onApplyClick = {},
            onCancelClick = {},
            onDeleteFilter = {},
            onClearHistoryClick = {},
            onClearHistoryConfirm = {},
            onClearHistoryDismiss = {}
        )
    }
}
