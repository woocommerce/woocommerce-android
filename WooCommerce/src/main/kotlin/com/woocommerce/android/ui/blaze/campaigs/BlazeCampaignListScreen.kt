package com.woocommerce.android.ui.blaze.campaigs

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.CampaignStatusUi.Active
import com.woocommerce.android.ui.blaze.campaigs.BlazeCampaignListViewModel.BlazeCampaignListState
import com.woocommerce.android.ui.blaze.campaigs.BlazeCampaignListViewModel.ClickableCampaign
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.WCColoredButton

@Composable
fun BlazeCampaignListScreen(viewModel: BlazeCampaignListViewModel) {
    viewModel.state.observeAsState().value?.let { state ->
        BlazeCampaignListScreen(
            state = state,
            modifier = Modifier.background(color = MaterialTheme.colors.surface),
            onEndOfTheListReached = viewModel::onLoadMoreCampaigns,
            onCampaignCelebrationDismissed = viewModel::onCampaignCelebrationDismissed,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BlazeCampaignListScreen(
    state: BlazeCampaignListState,
    modifier: Modifier = Modifier,
    onEndOfTheListReached: () -> Unit,
    onCampaignCelebrationDismissed: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = {
            if (it == ModalBottomSheetValue.Hidden) {
                onCampaignCelebrationDismissed()
            }

            it != ModalBottomSheetValue.HalfExpanded
        }
    )

    LaunchedEffect(state.isCampaignCelebrationShown) {
        if (state.isCampaignCelebrationShown) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = {
            CampaignCelebrationSheet(onCampaignCelebrationDismissed)
        },
        sheetShape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.corner_radius_large),
            topEnd = dimensionResource(id = R.dimen.corner_radius_large)
        ),
        content = {
            CampaignList(
                state = state,
                onEndOfTheListReached = onEndOfTheListReached,
                modifier = modifier
            )
        }
    )
}

@Composable
private fun CampaignList(
    state: BlazeCampaignListState,
    onEndOfTheListReached: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = dimensionResource(id = R.dimen.major_100),
                start = dimensionResource(id = R.dimen.major_100),
                end = dimensionResource(id = R.dimen.major_100),
            )
    ) {
        val listState = rememberLazyListState()
        LazyColumn(state = listState) {
            items(state.campaigns) { campaign ->
                BlazeCampaignItem(
                    campaign = campaign.campaignUi,
                    onCampaignClicked = campaign.onCampaignClicked,
                )
                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.major_100)))
            }
            if (state.isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth()
                            .padding(vertical = dimensionResource(id = R.dimen.minor_100))
                    )
                }
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
                imageVector = Filled.Add,
                contentDescription = "Large floating action button",
            )
        }
        InfiniteListHandler(listState = listState) {
            onEndOfTheListReached()
        }
    }
}

@Composable
private fun CampaignCelebrationSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_50)))
        BottomSheetHandle()
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
        Box(
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.major_400))
                .border(
                    width = dimensionResource(id = R.dimen.minor_100),
                    color = colorResource(id = R.color.woo_green_50).copy(alpha = 0.5f),
                    shape = CircleShape
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_rounded),
                tint = colorResource(id = R.color.woo_green_50),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 42.dp, height = 32.dp)
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
        Text(
            text = stringResource(id = R.string.blaze_campaign_creation_celebration_header),
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = R.string.blaze_campaign_ceation_celebration_message),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        WCColoredButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.blaze_campaign_ceation_celebration_button))
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
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
                ClickableCampaign(
                    BlazeCampaignUi(
                        product = BlazeProductUi(
                            name = "Product name",
                            imgUrl = "https://picsum.photos/200/300",
                        ),
                        status = Active,
                        isEndlessCampaign = false,
                        impressions = 100,
                        clicks = 10,
                        formattedBudget = "$100",
                        budgetLabel = R.string.blaze_campaign_status_budget_total
                    ),
                    onCampaignClicked = {}
                )
            ),
            onAddNewCampaignClicked = {},
            isLoading = false,
            isCampaignCelebrationShown = false
        ),
        onEndOfTheListReached = {},
        onCampaignCelebrationDismissed = {},
    )
}
