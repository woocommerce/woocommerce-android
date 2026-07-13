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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.dashboard.DashboardSkeleton
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
    viewModel: DashboardGoogleAdsViewModel = hiltViewModel { factory: DashboardGoogleAdsViewModel.Factory ->
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
                        title = webViewTitle,
                        urlComparisonMode = GoogleAdsWebViewViewModel.UrlComparisonMode.PARTIAL,
                        isCreationFlow = event.isCreationFlow,
                        entryPointSource = GoogleAdsWebViewViewModel.EntryPointSource.MYSTORE
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
    val roundedShape = RoundedCornerShape(WooTheme.radius.medium)
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .border(
                    width = dimensionResource(id = R.dimen.minor_10),
                    color = WooTheme.colors.outlineVariant,
                    shape = roundedShape,
                )
                .clip(roundedShape)
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.major_100)
                )
                .background(WooTheme.colors.surface.default)
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
                DashboardSkeleton(width = 200.dp, height = 24.dp)
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.minor_100)))
                DashboardSkeleton(width = 250.dp, height = 16.dp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .border(
                width = dimensionResource(id = R.dimen.minor_10),
                color = WooTheme.colors.outlineVariant,
                shape = roundedShape,
            )
            .clip(roundedShape)
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.major_100)
            )
            .background(WooTheme.colors.surface.default)
            .fillMaxWidth()
    ) {
        DashboardSkeleton(width = 200.dp, height = 24.dp)
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun GoogleAdsNoCampaigns(
    onCreateCampaignClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roundedShape = RoundedCornerShape(WooTheme.radius.medium)
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier
                .border(
                    width = dimensionResource(id = R.dimen.minor_10),
                    color = WooTheme.colors.outlineVariant,
                    shape = roundedShape,
                )
                .clip(roundedShape)
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.major_100)
                )
                .background(WooTheme.colors.surface.default)
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
                    style = WooTheme.text.titleLarge.strong,
                    color = WooTheme.colors.surface.onDefault,
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.minor_100)))
                Text(
                    text = stringResource(R.string.dashboard_google_ads_card_no_campaign_description),
                    style = WooTheme.text.bodyLarge.regular,
                    color = WooTheme.colors.surface.onDefault,
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
    val roundedShape = RoundedCornerShape(WooTheme.radius.medium)
    Column {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = modifier
                .border(
                    width = dimensionResource(id = R.dimen.minor_10),
                    color = WooTheme.colors.outlineVariant,
                    shape = roundedShape,
                )
                .clip(roundedShape)
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.major_100)
                )
                .background(WooTheme.colors.surface.default)
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
                        style = WooTheme.text.bodyLarge.emphasized,
                        color = WooTheme.colors.surface.onDefault,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = WooTheme.colors.surface.onVariant,
                    )
                }
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.major_100)))

                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dashboard_google_ads_card_has_campaign_impressions),
                            style = WooTheme.text.bodyMedium.regular,
                            color = WooTheme.colors.surface.onDefault,
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                        Text(
                            text = impressions,
                            style = WooTheme.text.headlineSmall.emphasized,
                            color = WooTheme.colors.surface.onDefault,
                        )
                    }

                    Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dashboard_google_ads_card_has_campaign_clicks),
                            style = WooTheme.text.bodyMedium.regular,
                            color = WooTheme.colors.surface.onDefault,
                        )
                        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                        Text(
                            text = clicks,
                            style = WooTheme.text.headlineSmall.emphasized,
                            color = WooTheme.colors.surface.onDefault,
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
    WooOutlinedButton(
        text = stringResource(R.string.dashboard_google_ads_card_create_campaign_button),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = dimensionResource(id = R.dimen.minor_100),
                bottom = dimensionResource(id = R.dimen.major_100)
            ),
        onClick = onClick,
    )
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Preview(name = "RTL", locale = "ar")
@Composable
private fun DashboardGoogleAdsNoCampaignsPreview() {
    WooDesignSystemThemeWithBackground {
        DashboardGoogleAdsView(
            viewState = DashboardGoogleAdsState.NoCampaigns(
                onCreateCampaignClicked = {},
                menu = DashboardViewModel.DashboardWidgetMenu(emptyList()),
            ),
            onContactSupportClicked = {},
            onRetryOnErrorButtonClicked = {},
            modifier = Modifier,
        )
    }
}
