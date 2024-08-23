package com.woocommerce.android.ui.customfields.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
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
            onCustomFieldValueClicked = viewModel::onCustomFieldValueClicked,
            onBackClick = viewModel::onBackClick
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CustomFieldsScreen(
    state: CustomFieldsViewModel.UiState,
    onPullToRefresh: () -> Unit,
    onCustomFieldValueClicked: (CustomFieldUiModel) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.custom_fields_list_title),
                onNavigationButtonClick = onBackClick
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        val pullToRefreshState = rememberPullRefreshState(state.isLoading, onPullToRefresh)

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .pullRefresh(state = pullToRefreshState)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.customFields) { customField ->
                    CustomFieldItem(
                        customField = customField,
                        onValueClicked = onCustomFieldValueClicked,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Divider()
                }
            }

            PullRefreshIndicator(
                refreshing = state.isLoading,
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun CustomFieldItem(
    customField: CustomFieldUiModel,
    onValueClicked: (CustomFieldUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(16.dp)
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
                isLoading = false
            ),
            onPullToRefresh = {},
            onCustomFieldValueClicked = {},
            onBackClick = {}
        )
    }
}
