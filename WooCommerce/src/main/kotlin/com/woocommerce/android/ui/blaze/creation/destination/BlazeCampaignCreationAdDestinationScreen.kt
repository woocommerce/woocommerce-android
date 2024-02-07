package com.woocommerce.android.ui.blaze.creation.destination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationViewModel.ViewState
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BlazeCampaignCreationAdDestinationScreen(viewModel: BlazeCampaignCreationAdDestinationViewModel) {
    viewModel.viewState.observeAsState().value?.let { previewState ->
        AdDestinationScreen(
            previewState,
            viewModel::onBackPressed,
            viewModel::onUrlPropertyTapped,
            viewModel::onParameterPropertyTapped
        )
    }
}

@Composable
fun AdDestinationScreen(
    viewState: ViewState,
    onBackPressed: () -> Unit,
    onUrlPropertyTapped: () -> Unit,
    onParametersPropertyTapped: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.blaze_campaign_preview_details_destination_url),
                onNavigationButtonClick = onBackPressed,
                navigationIcon = Filled.ArrowBack
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
                value = viewState.destinationUrl,
                onPropertyTapped = onUrlPropertyTapped
            )
            Divider()
            AdDestinationProperty(
                title = stringResource(id = R.string.blaze_campaign_edit_ad_destination_parameters_property_title),
                value = viewState.parameters,
                onPropertyTapped = onParametersPropertyTapped
            )
        }
    }
}

@Composable
fun AdDestinationProperty(title: String, value: String, onPropertyTapped: () -> Unit) {
    Column(
        modifier = Modifier
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

@LightDarkThemePreviews
@Composable
fun PreviewAdDestinationScreen() {
    WooThemeWithBackground {
        AdDestinationScreen(
            viewState = ViewState(
                destinationUrl = "https://woocommerce.com",
                parameters = "utm_source=woocommerce\nutm_medium=android\nutm_campaign=blaze"
            ),
            onBackPressed = {},
            onUrlPropertyTapped = {},
            onParametersPropertyTapped = {}
        )
    }
}
