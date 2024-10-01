package com.woocommerce.android.ui.customfields.list

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.DiscardChangesDialog
import com.woocommerce.android.ui.compose.component.ExpandableTopBanner
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.customfields.CustomField
import com.woocommerce.android.ui.customfields.CustomFieldContentType
import com.woocommerce.android.ui.customfields.CustomFieldUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun CustomFieldsScreen(
    viewModel: CustomFieldsViewModel,
    snackbarHostState: SnackbarHostState
) {
    viewModel.state.observeAsState().value?.let { state ->
        CustomFieldsScreen(
            state = state,
            onPullToRefresh = viewModel::onPullToRefresh,
            onSaveClicked = viewModel::onSaveClicked,
            onCustomFieldClicked = viewModel::onCustomFieldClicked,
            onCustomFieldValueClicked = viewModel::onCustomFieldValueClicked,
            onAddCustomFieldClicked = viewModel::onAddCustomFieldClicked,
            onBackClick = viewModel::onBackClick,
            snackbarHostState = snackbarHostState
        )
    }

    viewModel.overlayedField.observeAsState().value?.let { overlayedField ->
        JsonCustomFieldViewer(
            customField = overlayedField,
            onDismiss = viewModel::onOverlayedFieldDismissed
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CustomFieldsScreen(
    state: CustomFieldsViewModel.UiState,
    onPullToRefresh: () -> Unit,
    onSaveClicked: () -> Unit,
    onCustomFieldClicked: (CustomFieldUiModel) -> Unit,
    onCustomFieldValueClicked: (CustomFieldUiModel) -> Unit,
    onAddCustomFieldClicked: () -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    BackHandler { onBackClick() }

    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.custom_fields_list_title),
                onNavigationButtonClick = onBackClick,
                actions = {
                    if (state.hasChanges) {
                        WCTextButton(
                            text = stringResource(id = R.string.save),
                            onClick = onSaveClicked
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomFieldClicked,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.custom_fields_add_button)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        val pullToRefreshState = rememberPullRefreshState(state.isRefreshing, onPullToRefresh)

        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            if (state.topBannerState != null) {
                ExpandableTopBanner(
                    title = stringResource(id = R.string.custom_fields_list_top_banner_title),
                    message = stringResource(id = R.string.custom_fields_list_top_banner_message),
                    onDismiss = state.topBannerState.onDismiss,
                    expandedByDefault = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .pullRefresh(state = pullToRefreshState)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.customFields) { customField ->
                        CustomFieldItem(
                            customField = customField,
                            onClicked = onCustomFieldClicked,
                            onValueClicked = onCustomFieldValueClicked,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Divider()
                    }
                }

                PullRefreshIndicator(
                    refreshing = state.isRefreshing,
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        if (state.isSaving) {
            ProgressDialog(
                title = stringResource(id = R.string.custom_fields_list_progress_dialog_title),
                subtitle = stringResource(id = R.string.please_wait)
            )
        }

        state.discardChangesDialogState?.let {
            DiscardChangesDialog(
                discardButton = it.onDiscard,
                dismissButton = it.onCancel
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun CustomFieldItem(
    customField: CustomFieldUiModel,
    onClicked: (CustomFieldUiModel) -> Unit,
    onValueClicked: (CustomFieldUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = { onClicked(customField) })
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = customField.key,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.subtitle2
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (customField.contentType != CustomFieldContentType.TEXT) {
                val text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colors.primary)) {
                        pushUrlAnnotation(UrlAnnotation(customField.value))
                        append(customField.value)
                    }
                }
                ClickableText(
                    text = text,
                    style = MaterialTheme.typography.body2.copy(
                        color = MaterialTheme.colors.onSurface
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    onClick = { offset ->
                        text.getUrlAnnotations(
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { _ ->
                            onValueClicked(customField)
                        }
                    }
                )
            } else {
                Text(
                    text = customField.value,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.body2
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Default.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(16.dp)
        )
    }
}

@Composable
private fun JsonCustomFieldViewer(
    customField: CustomFieldUiModel,
    onDismiss: () -> Unit
) {
    // We use this to disable focus on the text fields used to show the key and value as it's not needed for our case
    val inactiveInteractionSource = remember {
        object : MutableInteractionSource {
            override val interactions: Flow<Interaction> = emptyFlow()
            override suspend fun emit(interaction: Interaction) {}
            override fun tryEmit(interaction: Interaction): Boolean = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            val jsonFormatted = remember(customField.value) {
                runCatching {
                    if (customField.value.trimStart().startsWith("[")) {
                        JSONArray(customField.value).toString(4)
                    } else {
                        JSONObject(customField.value).toString(4)
                    }
                }.getOrDefault(customField.value)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                OutlinedTextField(
                    value = customField.key,
                    onValueChange = {},
                    label = { Text(text = stringResource(id = R.string.custom_fields_editor_key_label)) },
                    readOnly = true,
                    interactionSource = inactiveInteractionSource,
                    modifier = Modifier.focusable(enabled = false)
                )

                OutlinedTextField(
                    value = jsonFormatted,
                    onValueChange = {},
                    label = { Text(text = stringResource(id = R.string.custom_fields_editor_value_label)) },
                    readOnly = true,
                    interactionSource = inactiveInteractionSource,
                    modifier = Modifier.weight(1f, fill = false)
                )

                WCTextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = stringResource(id = R.string.close))
                }
            }
        }
    }
}

@LightDarkThemePreviews
@Preview
@Composable
private fun CustomFieldsScreenPreview() {
    WooThemeWithBackground {
        CustomFieldsScreen(
            state = CustomFieldsViewModel.UiState(
                customFields = listOf(
                    CustomFieldUiModel(CustomField(0, "key1", "Value 1")),
                    CustomFieldUiModel(CustomField(1, "key2", "Value 2")),
                    CustomFieldUiModel(
                        CustomField(
                            id = 2,
                            key = "key3",
                            value = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                        )
                    ),
                    CustomFieldUiModel(CustomField(3, "key4", "https://url.com")),
                ),
                topBannerState = CustomFieldsViewModel.TopBannerState { }
            ),
            onPullToRefresh = {},
            onSaveClicked = {},
            onCustomFieldClicked = {},
            onCustomFieldValueClicked = {},
            onAddCustomFieldClicked = {},
            onBackClick = {}
        )
    }
}

@LightDarkThemePreviews
@Preview
@Composable
private fun JsonCustomFieldViewerPreview() {
    WooThemeWithBackground {
        JsonCustomFieldViewer(
            customField = CustomFieldUiModel(
                CustomField(
                    id = 0,
                    key = "key1",
                    value = "[{\"key\": \"value\"}]"
                )
            ),
            onDismiss = {}
        )
    }
}
