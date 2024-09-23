package com.woocommerce.android.ui.customfields.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Colors
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
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
            onEditorModeChanged = viewModel::onEditorModeChanged,
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
    onEditorModeChanged: (Boolean) -> Unit,
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

            Toggle(
                useHtmlEditor = state.useHtmlEditor,
                onToggle = onEditorModeChanged,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box {
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.useHtmlEditor,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OutlinedAztecEditor(
                        content = state.customField.value,
                        onContentChanged = onValueChanged,
                        label = stringResource(R.string.custom_fields_editor_value_label),
                        enableSourceEditor = false,
                        minLines = 5
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = !state.useHtmlEditor,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    WCOutlinedTextField(
                        value = state.customField.value,
                        onValueChange = onValueChanged,
                        label = stringResource(R.string.custom_fields_editor_value_label),
                        minLines = 5
                    )
                }
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

@Composable
private fun Toggle(
    useHtmlEditor: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentDescription = stringResource(R.string.custom_fields_editor_toggle_accessibility_description)
    val state = if (useHtmlEditor) {
        stringResource(R.string.custom_fields_editor_html_toggle)
    } else {
        stringResource(R.string.custom_fields_editor_text_toggle)
    }

    Box(
        modifier = modifier
            .toggleable(
                value = useHtmlEditor,
                onValueChange = onToggle
            )
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                this.stateDescription = state
            }
            .background(
                MaterialTheme.colors.toggleBackgroundColor,
                MaterialTheme.shapes.medium
            )
    ) {
        var size by remember { mutableStateOf(DpSize.Zero) }

        val animationStiffness = 100f

        val offset by animateDpAsState(
            targetValue = if (useHtmlEditor) size.width else 0.dp,
            animationSpec = spring(stiffness = animationStiffness),
            label = "offset"
        )
        val textAlpha by animateFloatAsState(
            targetValue = if (useHtmlEditor) 1f else 0.5f,
            animationSpec = spring(stiffness = animationStiffness),
            label = "text alpha"
        )
        val htmlTextColor by animateColorAsState(
            targetValue = if (useHtmlEditor) MaterialTheme.colors.onPrimary else LocalContentColor.current,
            animationSpec = spring(stiffness = animationStiffness),
            label = "html text color"
        )
        val textTextColor by animateColorAsState(
            targetValue = if (useHtmlEditor) LocalContentColor.current else MaterialTheme.colors.onPrimary,
            animationSpec = spring(stiffness = animationStiffness),
            label = "regular text color"
        )

        val density = LocalDensity.current

        val boxShape = MaterialTheme.shapes.medium
        Box(
            modifier = Modifier
                .size(size)
                .offset(x = offset)
                .shadow(1.dp, MaterialTheme.shapes.medium)
                .background(
                    MaterialTheme.colors.primary,
                    boxShape
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(IntrinsicSize.Max)
        ) {
            Text(
                text = stringResource(R.string.custom_fields_editor_text_toggle),
                color = textTextColor.copy(alpha = 1.5f - textAlpha),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .defaultMinSize(minWidth = 128.dp)
                    .onSizeChanged {
                        size = with(density) {
                            DpSize(it.width.toDp(), it.height.toDp())
                        }
                    }
                    .clickable(onClick = { onToggle(false) })
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .weight(1f)
            )
            Text(
                text = stringResource(R.string.custom_fields_editor_html_toggle),
                color = htmlTextColor.copy(alpha = textAlpha),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable(onClick = { onToggle(true) })
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .weight(1f)
            )
        }
    }
}

private val Colors.toggleBackgroundColor: Color
    @Composable
    get() = if (isLight) MaterialTheme.colors.background else Color.DarkGray

@LightDarkThemePreviews
@Preview
@Composable
private fun CustomFieldsEditorScreenPreview() {
    var useHtmlEditor by remember { mutableStateOf(false) }
    WooThemeWithBackground {
        CustomFieldsEditorScreen(
            CustomFieldsEditorViewModel.UiState(
                customField = CustomFieldUiModel("key", "value"),
                useHtmlEditor = useHtmlEditor,
            ),
            onKeyChanged = {},
            onValueChanged = {},
            onDoneClicked = {},
            onDeleteClicked = {},
            onCopyKeyClicked = {},
            onCopyValueClicked = {},
            onEditorModeChanged = { useHtmlEditor = it },
            onBackButtonClick = {}
        )
    }
}
