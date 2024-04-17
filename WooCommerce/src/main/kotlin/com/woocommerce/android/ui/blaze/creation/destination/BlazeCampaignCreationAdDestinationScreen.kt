package com.woocommerce.android.ui.blaze.creation.destination

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationViewModel.ViewState
import com.woocommerce.android.ui.compose.component.DialogButtonsRowLayout
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BlazeCampaignCreationAdDestinationScreen(viewModel: BlazeCampaignCreationAdDestinationViewModel) {
    viewModel.viewState.observeAsState().value?.let { previewState ->
        AdDestinationScreen(
            previewState,
            viewModel::onBackPressed,
            viewModel::onUrlPropertyTapped,
            viewModel::onParameterPropertyTapped,
            viewModel::onDestinationParametersUpdated
        )
    }
}

@Composable
fun AdDestinationScreen(
    viewState: ViewState,
    onBackPressed: () -> Unit,
    onUrlPropertyTapped: () -> Unit,
    onParametersPropertyTapped: () -> Unit,
    onTargetUrlChanged: (String) -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.blaze_campaign_preview_details_destination_url),
                onNavigationButtonClick = onBackPressed,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
            )
        },
        modifier = Modifier.background(MaterialTheme.colors.surface)
    ) { paddingValues ->
        Column(
            Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AdDestinationProperty(
                title = stringResource(id = R.string.blaze_campaign_edit_ad_destination_url_property_title),
                value = viewState.targetUrl,
                onPropertyTapped = onUrlPropertyTapped
            )
            Divider()
            AdDestinationProperty(
                title = stringResource(id = R.string.blaze_campaign_edit_ad_destination_parameters_property_title),
                value = viewState.joinedParameters.ifBlank {
                    stringResource(R.string.blaze_campaign_edit_ad_destination_empty_parameters_message)
                },
                onPropertyTapped = onParametersPropertyTapped
            )
        }

        if (viewState.isUrlDialogVisible) {
            TargetUrlDialog(
                viewState,
                onDismissed = { onTargetUrlChanged(viewState.targetUrl) },
                onSaveTapped = onTargetUrlChanged
            )
        }
    }
}

@Composable
fun AdDestinationProperty(title: String, value: String, onPropertyTapped: () -> Unit) {
    Column(
        modifier =
        Modifier
            .clickable { onPropertyTapped() }
            .padding(dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth(1f)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_high)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_on_surface_medium),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TargetUrlDialog(
    viewState: ViewState,
    onDismissed: () -> Unit,
    onSaveTapped: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismissed) {
        Column(
            modifier =
            Modifier
                .background(MaterialTheme.colors.surface, MaterialTheme.shapes.medium)
                .padding(dimensionResource(id = R.dimen.major_100)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = R.string.blaze_campaign_edit_ad_destination_url_property_title),
                style = MaterialTheme.typography.h6
            )

            var targetUrl by rememberSaveable {
                mutableStateOf(viewState.targetUrl)
            }

            UrlOption(
                url = viewState.productUrl,
                targetUrl = targetUrl,
                title = R.string.blaze_campaign_edit_ad_destination_product_url_option
            ) {
                targetUrl = viewState.productUrl
            }

            UrlOption(
                url = viewState.siteUrl,
                targetUrl = targetUrl,
                title = R.string.blaze_campaign_edit_ad_destination_site_url_option
            ) {
                targetUrl = viewState.siteUrl
            }

            DialogButtonsRowLayout(
                confirmButton = {
                    WCTextButton(onClick = {
                        onSaveTapped(targetUrl)
                    }) {
                        Text(text = stringResource(id = R.string.save))
                    }
                },
                dismissButton = {
                    WCTextButton(onClick = onDismissed) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                },
                neutralButton = null
            )
        }
    }
}

@Composable
private fun UrlOption(
    url: String,
    targetUrl: String,
    @StringRes title: Int,
    onOptionSelected: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = interactionSource,
            onClick = onOptionSelected
        ),
    ) {
        RadioButton(
            selected = url == targetUrl,
            onClick = onOptionSelected,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colors.primary,
                unselectedColor = colorResource(id = R.color.color_on_surface_medium)
            )
        )
        Column {
            Text(stringResource(title))
            Text(
                text = url,
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
fun PreviewAdDestinationScreen() {
    WooThemeWithBackground {
        AdDestinationScreen(
            viewState = ViewState(
                productUrl = "https://woocommerce.com/products/1",
                siteUrl = "https://woocommerce.com",
                targetUrl = "https://woocommerce.com/products/12",
                parameters = emptyMap(),
                isUrlDialogVisible = true
            ),
            onBackPressed = {},
            onUrlPropertyTapped = {},
            onParametersPropertyTapped = {},
            onTargetUrlChanged = {}
        )
    }
}
