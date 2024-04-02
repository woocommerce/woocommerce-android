package com.woocommerce.android.ui.mystore.widgeteditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.analytics.hub.settings.LoadWidgetsConfiguration
import com.woocommerce.android.ui.compose.component.DiscardChangesDialog
import com.woocommerce.android.ui.compose.component.DragAndDropItemsList

@Composable
fun MyStoreWidgetEditorScreen(viewModel: MyStoreWidgetEditorViewModel) {
    BackHandler(onBack = viewModel::onBackPressed)
    viewModel.viewState.observeAsState().value?.let { state ->
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.my_store_edit_screen_widgets)) },
                navigationIcon = {
                    IconButton(viewModel::onBackPressed) {
                        Icon(
                            Filled.Close,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                backgroundColor = colorResource(id = R.color.color_toolbar),
                actions = {
                    TextButton(
                        onClick = viewModel::onSaveClicked,
                        enabled = state.isSaveButtonEnabled
                    ) {
                        Text(
                            text = stringResource(id = R.string.save).uppercase()
                        )
                    }
                },
            )
        }) { padding ->
            when {
                state.isLoading -> LoadWidgetsConfiguration()
                else -> {
                    DragAndDropItemsList(
                        items = state.widgetList,
                        selectedItems = state.widgetList.filter { it.isSelected },
                        onSelectionChange = viewModel::onSelectionChange,
                        onOrderChange = viewModel::onOrderChange,
                        itemFormatter = { title },
                        itemKey = { _, widget -> widget.type },
                        modifier = Modifier.padding(padding)
                    )

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
