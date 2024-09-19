package com.woocommerce.android.ui.customfields.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.DiscardChangesDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCOverflowMenu
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.component.aztec.OutlinedAztecEditor
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.customfields.CustomFieldUiModel

@Composable
fun CustomFieldsEditorScreen(viewModel: CustomFieldsEditorViewModel) {
    viewModel.state.observeAsState().value?.let { state ->
        CustomFieldsEditorScreen(
            state = state,
            onKeyChanged = viewModel::onKeyChanged,
            onValueChanged = viewModel::onValueChanged,
            onDoneClicked = viewModel::onDoneClicked,
            onDeleteClicked = viewModel::onDeleteClicked,
            onCopyKeyClicked = viewModel::onCopyKeyClicked,
            onCopyValueClicked = viewModel::onCopyValueClicked,
            onBackButtonClick = viewModel::onBackClick,
        )
    }
}

@Composable
private fun CustomFieldsEditorScreen(
    state: CustomFieldsEditorViewModel.UiState,
    onKeyChanged: (String) -> Unit,
    onValueChanged: (String) -> Unit,
    onDoneClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onCopyKeyClicked: () -> Unit,
    onCopyValueClicked: () -> Unit,
    onBackButtonClick: () -> Unit,
) {
    BackHandler { onBackButtonClick() }

    Scaffold(
        topBar = {
            Toolbar(
                title = "Custom Field",
                onNavigationButtonClick = onBackButtonClick,
                actions = {
                    if (state.showDoneButton) {
                        WCTextButton(
                            onClick = onDoneClicked,
                            text = stringResource(R.string.done)
                        )
                    }
                    WCOverflowMenu(
                        items = listOfNotNull(
                            R.string.custom_fields_editor_copy_key,
                            R.string.custom_fields_editor_copy_value,
                            if (!state.isCreatingNewItem) R.string.delete else null,
                        ),
                        mapper = { stringResource(it) },
                        itemColor = {
                            when (it) {
                                R.string.delete -> MaterialTheme.colors.error
                                else -> LocalContentColor.current
                            }
                        },
                        onSelected = { resourceId ->
                            when (resourceId) {
                                R.string.delete -> onDeleteClicked()
                                R.string.custom_fields_editor_copy_key -> onCopyKeyClicked()
                                R.string.custom_fields_editor_copy_value -> onCopyValueClicked()
                                else -> error("Unhandled menu item")
                            }
                        }
                    )
                }
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            WCOutlinedTextField(
                value = state.customField.key,
                onValueChange = onKeyChanged,
                label = stringResource(R.string.custom_fields_editor_key_label),
                helperText = state.keyErrorMessage?.getText(),
                isError = state.keyErrorMessage != null,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isHtml) {
                OutlinedAztecEditor(
                    content = state.customField.value,
                    onContentChanged = onValueChanged,
                    label = stringResource(R.string.custom_fields_editor_value_label),
                    minLines = 5
                )
            } else {
                WCOutlinedTextField(
                    value = state.customField.value,
                    onValueChange = onValueChanged,
                    label = stringResource(R.string.custom_fields_editor_value_label),
                    minLines = 5
                )
            }
        }

        state.discardChangesDialogState?.let {
            DiscardChangesDialog(
                discardButton = it.onDiscard,
                dismissButton = it.onCancel
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun CustomFieldsEditorScreenPreview() {
    WooThemeWithBackground {
        CustomFieldsEditorScreen(
            CustomFieldsEditorViewModel.UiState(customField = CustomFieldUiModel("key", "value")),
            onKeyChanged = {},
            onValueChanged = {},
            onDoneClicked = {},
            onDeleteClicked = {},
            onCopyKeyClicked = {},
            onCopyValueClicked = {},
            onBackButtonClick = {}
        )
    }
}
