package com.woocommerce.android.ui.blaze.creation.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel
import ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.AdDetailsUi
import ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.CampaignDetailItemUi
import ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.CampaignDetailsUi
import ui.blaze.creation.preview.BlazeCampaignCreationPreviewViewModel.CampaignPreviewUiState

@Composable
fun BlazeCampaignCreationPreviewScreen(viewModel: BlazeCampaignCreationPreviewViewModel) {
    viewModel.viewState.observeAsState().value?.let { previewState ->
        BlazeCampaignCreationPreviewScreen(
            previewState,
            viewModel::onBackPressed
        )
    }
}

@Composable
private fun BlazeCampaignCreationPreviewScreen(
    previewState: CampaignPreviewUiState,
    onBackPressed: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.blaze_campaign_screen_fragment_title),
                onNavigationButtonClick = onBackPressed,
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

            when {
                previewState.isLoading -> AdDetailsLoading()
                else -> AdDetailsHeader(state = previewState)
            }

            CampaignDetails(
                campaignDetails = previewState.campaignDetails,
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
                enabled = !previewState.isLoading
            )
        }
    }
}

@Composable
private fun AdDetailsLoading(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color = colorResource(id = R.color.blaze_campaign_preview_header_background)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SkeletonView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(276.dp)
                )
                SkeletonView(
                    modifier = Modifier
                        .width(120.dp)
                        .height(8.dp)
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    SkeletonView(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    SkeletonView(
                        modifier = Modifier
                            .width(80.dp)
                            .height(24.dp)
                    )
                }
            }
        }
        WCTextButton(
            modifier = Modifier.padding(bottom = 8.dp),
            onClick = { /* No action expected for disabled button */ },
            enabled = false,
        ) {
            Text(stringResource(id = R.string.blaze_campaign_preview_edit_ad_button))
        }
    }
}

@Composable
fun AdDetailsHeader(
    state: CampaignPreviewUiState,
    modifier: Modifier = Modifier,
) {
    CampaignHeader(
        adDetails = state.adDetails,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color = colorResource(id = R.color.blaze_campaign_preview_header_background))
    )
}

@Composable
fun CampaignHeader(adDetails: AdDetailsUi, modifier: Modifier = Modifier) {
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
                        .data(adDetails.campaignImageUrl)
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
                    text = adDetails.tagLine,
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
                        text = adDetails.title,
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
    campaignDetails: CampaignDetailsUi,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = stringResource(id = R.string.blaze_campaign_preview_details_section_title),
            style = MaterialTheme.typography.body2
        )
        // Budget
        CampaignPropertyGroupItem(items = listOf(campaignDetails.budget))
        Spacer(modifier = Modifier.height(16.dp))

        // Ad Audience
        CampaignPropertyGroupItem(items = campaignDetails.targetDetails)
        Spacer(modifier = Modifier.height(16.dp))

        // Destination
        CampaignPropertyGroupItem(items = listOf(campaignDetails.destinationUrl))
    }
}

@Composable
private fun CampaignPropertyGroupItem(
    items: List<CampaignDetailItemUi>,
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
            CampaignPropertyItem(item)
            if (index < items.lastIndex && items.size > 1) {
                Divider(color = borderColor, thickness = borderWidth)
            }
        }
    }
}

@Composable
private fun CampaignPropertyItem(
    item: CampaignDetailItemUi,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { item.onItemSelected() }
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
                text = item.displayTitle,
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_high)
            )
            Text(
                text = item.displayValue,
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium),
                maxLines = item.maxLinesValue ?: Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis
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
    BlazeCampaignCreationPreviewScreen(
        CampaignPreviewUiState(
            isLoading = false,
            adDetails = AdDetailsUi(
                productId = 123,
                title = "Get the latest white t-shirts",
                tagLine = "From 45.00 USD",
                campaignImageUrl = "https://rb.gy/gmjuwb",
            ),
            campaignDetails = CampaignDetailsUi(
                budget = CampaignDetailItemUi(
                    displayTitle = stringResource(R.string.blaze_campaign_preview_details_budget),
                    displayValue = "140 USD, 7 days from Jan 14",
                    onItemSelected = {},
                ),
                targetDetails = listOf(
                    CampaignDetailItemUi(
                        displayTitle = stringResource(R.string.blaze_campaign_preview_details_language),
                        displayValue = "English, Spanish",
                        onItemSelected = {},
                    ),
                    CampaignDetailItemUi(
                        displayTitle = stringResource(R.string.blaze_campaign_preview_details_devices),
                        displayValue = "USA, Poland, Japan",
                        onItemSelected = {},
                    ),
                    CampaignDetailItemUi(
                        displayTitle = stringResource(R.string.blaze_campaign_preview_details_location),
                        displayValue = "Samsung, Apple, Xiaomi",
                        onItemSelected = {},
                    ),
                    CampaignDetailItemUi(
                        displayTitle = stringResource(R.string.blaze_campaign_preview_details_interests),
                        displayValue = "Fashion, Clothing, T-shirts",
                        onItemSelected = {},
                    ),
                ),
                destinationUrl = CampaignDetailItemUi(
                    displayTitle = "Destination URL",
                    displayValue = "https://www.myer.com.au/p/white-t-shirt-797334760-797334760",
                    onItemSelected = {},
                    maxLinesValue = 1,
                )
            )
        ),
        onBackPressed = { }
    )
}

@LightDarkThemePreviews
@Composable
fun AdDetailsLoadingPreview() {
    AdDetailsLoading()
}
