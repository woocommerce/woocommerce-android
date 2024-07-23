package com.woocommerce.android.ui.dashboard.google

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.google.DashboardGoogleAdsViewModel.DashboardGoogleAdsState
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent

@Composable
fun DashboardGoogleAdsCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardGoogleAdsViewModel = viewModelWithFactory { factory: DashboardGoogleAdsViewModel.Factory ->
        factory.create(parentViewModel = parentViewModel)
    }
) {
    HandleEvents(event = viewModel.event)

    viewModel.viewState.observeAsState().value?.let { state ->
        DashboardGoogleAdsView(
            viewState = state,
            onContactSupportClicked = parentViewModel::onContactSupportClicked,
            onRetryOnErrorButtonClicked = viewModel::onRefresh,
            modifier = modifier
        )
    }
}

@Composable
private fun HandleEvents(
    event: LiveData<MultiLiveEvent.Event>
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    val webViewTitle = stringResource(id = R.string.more_menu_button_google)

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: MultiLiveEvent.Event ->
            when (event) {
                is DashboardGoogleAdsViewModel.ViewGoogleForWooEvent -> {
                    val direction = NavGraphMainDirections.actionGlobalGoogleAdsWebViewFragment(
                        urlToLoad = event.url,
                        urlsToTriggerExit = event.successUrls.toTypedArray(),
                        title = webViewTitle,
                        urlComparisonMode = GoogleAdsWebViewViewModel.UrlComparisonMode.PARTIAL
                    )

                    navController.navigateSafely(direction)
                }

                is DashboardGoogleAdsViewModel.NavigateToGoogleAdsSuccessEvent -> {
                    navController.navigateSafely(
                        NavGraphMainDirections.actionGlobalGoogleAdsCampaignSuccessBottomSheet()
                    )
                }
            }
        }

        event.observe(lifecycleOwner, observer)

        onDispose {
            event.removeObserver(observer)
        }
    }
}

@Composable
fun DashboardGoogleAdsView(
    viewState: DashboardGoogleAdsState,
    onContactSupportClicked: () -> Unit,
    onRetryOnErrorButtonClicked: () -> Unit,
    modifier: Modifier
) {
    WidgetCard(
        titleResource = DashboardWidget.Type.GOOGLE_ADS.titleResource,
        menu = viewState.menu,
        button = viewState.mainButton,
        isError = viewState is DashboardGoogleAdsState.Error,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                )
        ) {
            when (viewState) {
                is DashboardGoogleAdsState.Loading -> GoogleAdsLoading()
                is DashboardGoogleAdsState.NoCampaigns -> GoogleAdsNoCampaigns(viewState.onCreateCampaignClicked)
                is DashboardGoogleAdsState.HasCampaigns -> GoogleAdsHasCampaigns(
                    viewState.impressions,
                    viewState.clicks,
                    viewState.onCreateCampaignClicked,
                    viewState.onPerformanceAreaClicked
                )

                is DashboardGoogleAdsState.Error -> {
                    WidgetError(
                        onContactSupportClicked = onContactSupportClicked,
                        onRetryClicked = onRetryOnErrorButtonClicked
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleAdsLoading(
    modifier: Modifier = Modifier
) {
    val roundedShape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .border(
                    width = dimensionResource(id = R.dimen.minor_10),
                    color = colorResource(id = R.color.divider_color),
                    shape = roundedShape
                )
                .clip(roundedShape)
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.major_100)
                )
                .background(MaterialTheme.colors.surface)
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_250))
            )
            Column(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f)
            ) {
                SkeletonView(width = 200.dp, height = 24.dp)
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.minor_100)))
                SkeletonView(width = 250.dp, height = 16.dp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .border(
                width = dimensionResource(id = R.dimen.minor_10),
                color = colorResource(id = R.color.divider_color),
                shape = roundedShape
            )
            .clip(roundedShape)
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.major_100)
            )
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
    ) {
        SkeletonView(width = 200.dp, height = 24.dp)
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun GoogleAdsNoCampaigns(
    onCreateCampaignClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roundedShape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier
                .border(
                    width = dimensionResource(id = R.dimen.minor_10),
                    color = colorResource(id = R.color.divider_color),
                    shape = roundedShape
                )
                .clip(roundedShape)
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.major_100)
                )
                .background(MaterialTheme.colors.surface)
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_250))
            )
            Column(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.dashboard_google_ads_card_no_campaign_heading),
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.minor_100)))
                Text(
                    text = stringResource(R.string.dashboard_google_ads_card_no_campaign_description),
                    style = MaterialTheme.typography.body1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        CreateCampaignButton(onClick = onCreateCampaignClicked)
    }
}

@Composable
private fun GoogleAdsHasCampaigns(
    impressions: String,
    clicks: String,
    onCreateCampaignClicked: () -> Unit,
    onPerformanceAreaClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roundedShape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier
                .border(
                    width = dimensionResource(id = R.dimen.minor_10),
                    color = colorResource(id = R.color.divider_color),
                    shape = roundedShape
                )
                .clip(roundedShape)
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.major_100)
                )
                .background(MaterialTheme.colors.surface)
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_250))
            )
            Column(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f),
            ) {
                Row(
                    modifier = Modifier
                        .clickable {
                            onPerformanceAreaClicked()
                        }
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_google_ads_card_has_campaign_heading),
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.major_100)))

                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dashboard_google_ads_card_has_campaign_impressions),
                            style = MaterialTheme.typography.body2
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                        Text(
                            text = impressions,
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dashboard_google_ads_card_has_campaign_clicks),
                            style = MaterialTheme.typography.body2
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                        Text(
                            text = clicks,
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        CreateCampaignButton(onClick = onCreateCampaignClicked)
    }
}

@Composable
private fun CreateCampaignButton(onClick: () -> Unit) {
    WCOutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = dimensionResource(id = R.dimen.minor_100),
                bottom = dimensionResource(id = R.dimen.major_100)
            ),
        onClick = onClick
    ) {
        Text(stringResource(R.string.dashboard_google_ads_card_create_campaign_button))
    }
}
