package com.woocommerce.android.ui.customfields.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
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
    onBackButtonClick: () -> Unit,
) {
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
                }
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            WCOutlinedTextField(
                value = state.customField.key,
                onValueChange = onKeyChanged,
                label = "Key",
                placeholderText = "Enter key"
            )

            Spacer(modifier = Modifier.height(16.dp))

            WCOutlinedTextField(
                value = state.customField.value,
                onValueChange = onValueChanged,
                label = "Value",
                placeholderText = "Enter value",
                minLines = 5
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun CustomFieldsEditorScreenPreview() {
    WooThemeWithBackground {
        CustomFieldsEditorScreen(
            CustomFieldsEditorViewModel.UiState(
                customField = CustomFieldUiModel("key", "value"),
                showDoneButton = true,
                isHtml = false
            ),
            onKeyChanged = {},
            onValueChanged = {},
            onDoneClicked = {},
            onBackButtonClick = {}
        )
    }
}
