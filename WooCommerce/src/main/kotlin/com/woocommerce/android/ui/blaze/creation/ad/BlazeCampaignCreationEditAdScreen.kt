package com.woocommerce.android.ui.blaze.creation.ad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.CampaignPreviewUiState.CampaignDetailItem
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BlazeCampaignCreationPreviewScreen(viewModel: BlazeCampaignCreationEditAdViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        BlazeCampaignCreationEditAdScreen(viewState, viewModel::onTagLineChanged, viewModel::onDescriptionChanged)
    }
}

@Composable
private fun BlazeCampaignCreationEditAdScreen(
    viewState: BlazeCampaignCreationEditAdViewModel.ViewState,
    onTagLineChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.blaze_campaign_screen_fragment_title),
                onNavigationButtonClick = { /*TODO*/ },
                navigationIcon = Filled.ArrowBack
            )
        },
        modifier = Modifier.background(MaterialTheme.colors.surface)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(paddingValues)
                .background(color = MaterialTheme.colors.surface)
        ) {
            CampaignEditAdContent(viewState, onTagLineChanged, onDescriptionChanged)
        }
    }
}

@Composable
fun CampaignEditAdContent(
    viewState: BlazeCampaignCreationEditAdViewModel.ViewState,
    onTagLineChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(viewState.campaignImageUrl)
                .crossfade(true)
                .fallback(R.drawable.blaze_campaign_product_placeholder)
                .placeholder(R.drawable.blaze_campaign_product_placeholder)
                .error(R.drawable.blaze_campaign_product_placeholder)
                .build(),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.image_major_120))
                .clip(shape = RoundedCornerShape(size = 8.dp))
        )

        WCTextButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = { /*TODO*/ },
        ) {
            Text(stringResource(id = string.blaze_campaign_edit_ad_change_image_button))
        }

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_100))
        )

        WCOutlinedTextField(
            value = viewState.tagLine,
            onValueChange = onTagLineChanged,
            label = stringResource(id = string.blaze_campaign_edit_ad_change_tagline_title),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Text(
            text = stringResource(
                id = string.blaze_campaign_edit_ad_characters_remaining,
                viewState.taglineCharactersRemaining
            ),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_disabled),
            modifier = Modifier.fillMaxWidth()
        )

        WCOutlinedTextField(
            value = viewState.description,
            onValueChange = onDescriptionChanged,
            label = stringResource(id = string.blaze_campaign_edit_ad_change_description_title),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier
                .height(120.dp)
                .padding(top = dimensionResource(id = R.dimen.major_100))
        )

        Text(
            text = stringResource(
                id = string.blaze_campaign_edit_ad_characters_remaining,
                viewState.taglineCharactersRemaining
            ),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_disabled),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@LightDarkThemePreviews
@Preview
@Composable
fun PreviewCampaignEditAdContent() {
    WooThemeWithBackground {
        CampaignEditAdContent(
            viewState = BlazeCampaignCreationEditAdViewModel.ViewState(
                tagLine = "From 45.00 USD",
                description = "Get the latest white t-shirts",
                campaignImageUrl = "https://rb.gy/gmjuwb"
            ),
            onTagLineChanged = { },
            onDescriptionChanged = { }
        )
    }
}
