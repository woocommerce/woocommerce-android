package com.woocommerce.android.ui.blaze.campaigs

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.CampaignStatusUi.Active
import com.woocommerce.android.ui.blaze.campaigs.BlazeCampaignListViewModel.BlazeCampaignListState

@Composable
fun BlazeCampaignListScreen(viewModel: BlazeCampaignListViewModel) {
    viewModel.state.observeAsState().value?.let { state ->
        BlazeCampaignListScreen(
            state = state,
            onCampaignClicked = viewModel::onCampaignSelected,
            modifier = Modifier.background(color = MaterialTheme.colors.surface)
        )
    }
}

@Composable
private fun BlazeCampaignListScreen(
    state: BlazeCampaignListState,
    onCampaignClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {}
        else -> {
            LazyColumn(
                modifier = modifier.padding(dimensionResource(id = R.dimen.major_100))
            ) {
                items(state.campaigns) { campaign ->
                    BlazeCampaignItem(
                        campaign = campaign,
                        onCampaignClicked = onCampaignClicked,
                    )
                    Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_100)))
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
fun BlazeCampaignListScreen() {
    BlazeCampaignListScreen(
        state = BlazeCampaignListState(
            campaigns = listOf(
                BlazeCampaignUi(
                    product = BlazeProductUi(
                        name = "Product name",
                        imgUrl = "https://hips.hearstapps.com/hmg-prod/images/gh-082420-ghi-best-sofas-1598293488.png",
                    ),
                    status = Active,
                    impressions = 100,
                    clicks = 10,
                    budget = 1000
                )
            ),
            isLoading = false
        ),
        onCampaignClicked = { }
    )
}

