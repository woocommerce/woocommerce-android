package com.woocommerce.android.ui.dashboard.widgeteditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.analytics.hub.settings.LoadWidgetsConfiguration
import com.woocommerce.android.ui.compose.component.DiscardChangesDialog
import com.woocommerce.android.ui.compose.component.DragAndDropItemsList
import com.woocommerce.android.ui.compose.component.DragAndDropSelectableItem
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun DashboardWidgetEditorScreen(viewModel: DashboardWidgetEditorViewModel) {
    BackHandler(onBack = viewModel::onBackPressed)
    viewModel.viewState.observeAsState().value?.let { state ->
        Scaffold(topBar = {
            Toolbar(
                title = stringResource(id = R.string.my_store_edit_screen_widgets),
                onNavigationButtonClick = viewModel::onBackPressed,
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                actions = {
                    TextAction(
                        text = stringResource(id = R.string.save),
                        onClick = viewModel::onSaveClicked,
                        enabled = state.isSaveButtonEnabled,
                    )
                },
            )
        }) { padding ->
            when {
                state.isLoading -> LoadWidgetsConfiguration()
                else -> {
                    DragAndDropItemsList(
                        items = state.orderedWidgetList,
                        onOrderChange = viewModel::onOrderChange,
                        itemKey = { _, widget -> widget.type },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        isItemDraggable = { it.isAvailable }
                    ) { item, dragDropState ->
                        when (item.isAvailable) {
                            true -> {
                                val selectedItems = state.orderedWidgetList.filter { it.isVisible }
                                DragAndDropSelectableItem(
                                    item = item,
                                    isSelected = item.isSelected,
                                    dragDropState = dragDropState,
                                    onSelectionChange = viewModel::onSelectionChange,
                                    itemKey = { it.type },
                                    itemFormatter = { stringResource(id = item.title) },
                                    isEnabled = !item.isSelected || selectedItems.size > 1
                                )
                            }

                            false -> {
                                UnavailableWidget(item)
                            }
                        }
                    }

                    if (state.showDiscardDialog) {
                        DiscardChangesDialog(
                            dismissButton = viewModel::onDismissDiscardDialog,
                            discardButton = viewModel::onDiscardChanges
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnavailableWidget(
    widget: DashboardWidget,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(16.dp)
    ) {
        Spacer(modifier = Modifier.size(24.dp))

        Text(
            text = stringResource(id = widget.title),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        Box(
            modifier = Modifier
                .background(Color.Gray, RoundedCornerShape(4.dp))
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            val widgetStatus = widget.status as DashboardWidget.Status.Unavailable

            Text(
                text = stringResource(id = widgetStatus.badgeText),
                color = Color.White,
                style = MaterialTheme.typography.caption
            )
        }
    }
}
