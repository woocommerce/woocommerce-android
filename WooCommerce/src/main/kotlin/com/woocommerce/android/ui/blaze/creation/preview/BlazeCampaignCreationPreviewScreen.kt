package com.woocommerce.android.ui.blaze.creation.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.CampaignPreviewState.CampaignPreviewContent
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.CampaignPreviewState.Loading
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BlazeCampaignCreationPreviewScreen(viewModel: BlazeCampaignCreationPreviewViewModel) {
    viewModel.viewState.observeAsState().value?.let { previewState ->
        WooThemeWithBackground {
            Scaffold(
                topBar = {
                    Toolbar(
                        title = stringResource(id = R.string.more_menu_button_blaze),
                        onNavigationButtonClick = {/*TODO*/ },
                        navigationIcon = Filled.ArrowBack
                    )
                },
                modifier = Modifier.background(MaterialTheme.colors.surface)
            ) { paddingValues ->
                when (previewState) {
                    is Loading -> LoadingPreview()
                    is CampaignPreviewContent -> CampaignPreviewContent(
                        state = previewState,
                        modifier = Modifier
                            .padding(paddingValues)
                            .background(color = MaterialTheme.colors.surface)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingPreview() {
    TODO("Not yet implemented")
}

@Composable
fun CampaignPreviewContent(
    state: CampaignPreviewContent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(16.dp)
    ) {
        CampaignHeader(state)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CampaignHeader(state: CampaignPreviewContent, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color = colorResource(id = R.color.blaze_campaign_preview_header_background))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.campaignImageUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.ic_product)
                        .fallback(R.drawable.ic_product)
                        .error(R.drawable.ic_product)
                        .build(),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                        .fillMaxWidth()
                        .height(276.dp)
                        .clip(shape = RoundedCornerShape(size = 8.dp))
                )
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = state.tagLine,
                    style = MaterialTheme.typography.caption,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = state.title,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                    )
                    WCColoredButton(
                        text = stringResource(id = R.string.blaze_campaign_preview_shop_now_button),
                        modifier = Modifier
                            .padding(start = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            contentColor = colorResource(id = R.color.color_on_surface),
                            backgroundColor = colorResource(id = R.color.blaze_campaign_preview_header_background),
                        ),
                        onClick = { /*TODO*/ },
                    )
                }
            }
        }
        WCTextButton(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(id = R.string.blaze_campaign_preview_edit_ad_button),
            onClick = { /*TODO*/ },
        )
    }
}

@LightDarkThemePreviews
@Composable
fun CampaignScreenPreview() {
    CampaignPreviewContent(
        state = CampaignPreviewContent(
            productId = 1,
            title = "Very long title to see what happens with a long text",
            tagLine = "From $45.00",
            totalBudget = "totalBudget",
            duration = "duration",
            startDate = "startDate",
            languages = listOf("languages"),
            locations = listOf("locations"),
            devices = listOf("devices"),
            interests = listOf("interests"),
            addUrl = "addUrl",
            campaignImageUrl = "https://myer-media.com.au/wcsstore/MyerCatalogAssetStore/images/70/705/3856/100/1/797334760/797334760_1_720x928.webp",
        )
    )
}
