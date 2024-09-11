package com.woocommerce.android.ui.customfields.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.DiscardChangesDialog
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.customfields.CustomField
import com.woocommerce.android.ui.customfields.CustomFieldContentType
import com.woocommerce.android.ui.customfields.CustomFieldUiModel

@Composable
fun CustomFieldsScreen(viewModel: CustomFieldsViewModel) {
    viewModel.state.observeAsState().value?.let { state ->
        CustomFieldsScreen(
            state = state,
            onPullToRefresh = viewModel::onPullToRefresh,
            onSaveClicked = viewModel::onSaveClicked,
            onCustomFieldClicked = viewModel::onCustomFieldClicked,
            onCustomFieldValueClicked = viewModel::onCustomFieldValueClicked,
            onAddCustomFieldClicked = viewModel::onAddCustomFieldClicked,
            onBackClick = viewModel::onBackClick
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
    onBackClick: () -> Unit
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
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        val pullToRefreshState = rememberPullRefreshState(state.isRefreshing, onPullToRefresh)

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

            val text = buildAnnotatedString {
                if (customField.contentType != CustomFieldContentType.TEXT) {
                    pushUrlAnnotation(UrlAnnotation(customField.value))
                    pushStyle(SpanStyle(color = MaterialTheme.colors.primary))
                }
                append(customField.valueStrippedHtml)
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
                )
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
