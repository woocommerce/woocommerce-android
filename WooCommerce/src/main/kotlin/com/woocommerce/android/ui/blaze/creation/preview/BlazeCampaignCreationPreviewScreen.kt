package com.woocommerce.android.ui.blaze.creation.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
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
                        title = stringResource(id = R.string.blaze_campaign_screen_fragment_title),
                        onNavigationButtonClick = { /*TODO*/ },
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
    ) {
        CampaignHeader(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color = colorResource(id = R.color.blaze_campaign_preview_header_background))
        )
        CampaignDetails(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 8.dp),
            text = stringResource(id = R.string.blaze_campaign_preview_details_confirm_details_button),
            onClick = { /*TODO*/ },
        )
    }
}

@Composable
fun CampaignHeader(state: CampaignPreviewContent, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            backgroundColor = colorResource(id = R.color.woo_white),
            contentColor =
            if (isSystemInDarkTheme()) colorResource(id = R.color.color_surface)
            else colorResource(id = R.color.color_on_surface),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.campaignImageUrl)
                        .crossfade(true)
                        .build(),
                    fallback = painterResource(R.drawable.blaze_campaign_product_placeholder),
                    placeholder = painterResource(R.drawable.blaze_campaign_product_placeholder),
                    error = painterResource(R.drawable.blaze_campaign_product_placeholder),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
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
                            contentColor = colorResource(id = R.color.color_on_secondary),
                            backgroundColor = colorResource(id = R.color.blaze_campaign_preview_shop_now_button),
                        ),
                        onClick = { /*TODO*/ },
                    )
                }
            }
        }
        WCTextButton(
            modifier = Modifier.padding(top = 8.dp),
            onClick = { /*TODO*/ },
        ) {
            Text(stringResource(id = R.string.blaze_campaign_preview_edit_ad_button))
        }
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
        // Budget
        CampaignPropertyGroupItem(
            items = listOf(
                stringResource(id = R.string.blaze_campaign_preview_details_budget) to
                    state.budget.displayBudgetDetails,
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Audience
        CampaignPropertyGroupItem(
            items = listOf(
                stringResource(id = R.string.blaze_campaign_preview_details_language) to
                    state.audience.languages.joinToString(),
                stringResource(id = R.string.blaze_campaign_preview_details_devices) to
                    state.audience.devices.joinToString(),
                stringResource(id = R.string.blaze_campaign_preview_details_location) to
                    state.audience.locations.joinToString(),
                stringResource(id = R.string.blaze_campaign_preview_details_interests) to
                    state.audience.interests.joinToString(),
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Destination
        CampaignPropertyGroupItem(
            items = listOf(
                stringResource(id = R.string.blaze_campaign_preview_details_destination_url) to state.destinationUrl,
            )
        )
    }
}

@Composable
private fun CampaignPropertyGroupItem(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    val borderWidth = 1.dp
    val borderColor = colorResource(id = R.color.divider_color)
    Column(
        modifier = modifier
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        items.forEachIndexed { index, item ->
            CampaignPropertyItem(
                title = item.first,
                content = item.second,
            )
            if (index < items.lastIndex && items.size > 1) {
                Divider(color = borderColor, thickness = borderWidth)
            }
        }
    }
}

@Composable
private fun CampaignPropertyItem(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
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
                color = colorResource(id = R.color.color_on_surface_high)
            )
            Text(
                text = content,
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}

@LightDarkThemePreviews
@Composable
fun CampaignScreenPreview() {
    CampaignPreviewContent(
        state = CampaignPreviewContent(
            productId = 1,
            title = "Get the latest white t-shirts",
            tagLine = "From 45.00 USD",
            campaignImageUrl = "https://shorturl.at/bhqtz",
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
