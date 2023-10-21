package com.woocommerce.android.ui.blaze.campaigs

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.blaze.BlazeCampaignStat
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.CampaignStatusUi.Active
import com.woocommerce.android.ui.blaze.campaigs.BlazeCampaignListViewModel.BlazeCampaignListState
import com.woocommerce.android.ui.blaze.campaigs.BlazeCampaignListViewModel.CampaignState

@Composable
fun BlazeCampaignListScreen(viewModel: BlazeCampaignListViewModel) {
    viewModel.state.observeAsState().value?.let { state ->
        BlazeCampaignListScreen(
            state = state,
            modifier = Modifier.background(color = MaterialTheme.colors.surface)
        )
    }
}

@Composable
private fun BlazeCampaignListScreen(
    state: BlazeCampaignListState,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {}
        else -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(
                        top = dimensionResource(id = R.dimen.major_100),
                        start = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_100),
                    )
            ) {
                LazyColumn {
                    items(state.campaigns) { campaign ->
                        BlazeCampaignItem(
                            campaign = campaign.campaignUi,
                            onCampaignClicked = campaign.onCampaignClicked,
                        )
                        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_100)))
                    }
                }
                FloatingActionButton(
                    onClick = state.onAddNewCampaignClicked,
                    shape = CircleShape,
                    backgroundColor = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = dimensionResource(id = R.dimen.major_100))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Large floating action button",
                    )
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun BlazeCampaignListScreenPreview() {
    BlazeCampaignListScreen(
        state = BlazeCampaignListState(
            campaigns = listOf(
                CampaignState(
                    BlazeCampaignUi(
                        product = BlazeProductUi(
                            name = "Product name",
                            imgUrl = "https://picsum.photos/200/300",
                        ),
                        status = Active,
                        stats = listOf(
                            BlazeCampaignStat(
                                name = string.blaze_campaign_status_impressions,
                                value = 100
                            ),
                            BlazeCampaignStat(
                                name = string.blaze_campaign_status_clicks,
                                value = 10
                            ),
                            BlazeCampaignStat(
                                name = string.blaze_campaign_status_budget,
                                value = 1000
                            ),
                        ),
                    ),
                    onCampaignClicked = {}
                )
            ),
            onAddNewCampaignClicked = {},
            isLoading = false
        )
    )
}
