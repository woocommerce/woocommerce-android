package com.woocommerce.android.ui.blaze.creation.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.CampaignPreviewState
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
        CampaignHeader(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(color = colorResource(id = R.color.blaze_campaign_preview_header_background))
                .padding(16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        CampaignDetails(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        )
    }
}

@Composable
fun CampaignHeader(state: CampaignPreviewContent, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
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

@Composable
fun CampaignDetails(
    state: CampaignPreviewContent,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = stringResource(id = R.string.blaze_campaign_preview_details_section_title),
            style = MaterialTheme.typography.body2
        )
        CampaignPropertyItem(
            title = stringResource(id = R.string.blaze_campaign_preview_details_budget),
            content = state.budget.displayBudgetDetails
        )
    }
}

@Composable
private fun CampaignPropertyItem(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    val borderWidth = dimensionResource(id = R.dimen.minor_10)
    val borderColor = colorResource(id = R.color.divider_color)
    Row(
        modifier = modifier
            .padding(dimensionResource(id = dimen.major_100))
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .padding(end = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = color.color_on_surface_high)
            )
            Text(
                text = content,
                style = MaterialTheme.typography.body2,
                color = colorResource(id = color.color_on_surface_medium)
            )
        }
        Icon(
            painter = painterResource(id = drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(id = dimen.image_minor_50))
        )
    }
}

@LightDarkThemePreviews
@Composable
fun CampaignScreenPreview() {
    CampaignPreviewContent(
        state = CampaignPreviewState.CampaignPreviewContent(
            productId = 1,
            title = "Get the latest white t-shirts",
            tagLine = "From 45.00 USD",
            campaignImageUrl = "https://myer-media.com.au/wcsstore/MyerCatalogAssetStore/images/70/705/3856/100/1/797334760/797334760_1_720x928.webp",
            destinationUrl = "https://www.myer.com.au/p/white-t-shirt-797334760-797334760",
            budget = CampaignPreviewState.Budget(
                totalBudget = "140",
                duration = "7",
                startDate = "2024-10-01",
                displayBudgetDetails = "140 USD, 7 days from Jan 14"
            ),
            audience = CampaignPreviewState.Audience(
                languages = listOf("English"),
                locations = listOf("United States"),
                devices = listOf("Android"),
                interests = listOf("Fashion"),
            )
        )
    )
}
