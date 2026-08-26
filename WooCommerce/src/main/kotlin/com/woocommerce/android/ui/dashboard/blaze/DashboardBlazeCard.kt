package com.woocommerce.android.ui.dashboard.blaze

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.blaze.BlazeCampaignUi
import com.woocommerce.android.ui.blaze.BlazeProductUi
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.CampaignStatusUi
import com.woocommerce.android.ui.blaze.campaigs.BlazeCampaignItem
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardSkeleton
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.blaze.DashboardBlazeViewModel.DashboardBlazeCampaignState
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.main.MainActivityViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.launch

@Composable
fun DashboardBlazeCard(
    blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher,
    activityViewModel: MainActivityViewModel,
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardBlazeViewModel = hiltViewModel { factory: DashboardBlazeViewModel.Factory ->
        factory.create(parentViewModel)
    }
) {
    viewModel.blazeViewState.observeAsState().value?.let { state ->
        DashboardBlazeView(
            state = state,
            modifier = modifier,
            onContactSupportClicked = parentViewModel::onContactSupportClicked,
            onRetryOnErrorButtonClicked = viewModel::onRefresh
        )
    }

    HandleEvents(
        activityViewModel.event,
        viewModel.event,
        blazeCampaignCreationDispatcher
    )
}

@Composable
private fun HandleEvents(
    activityEvent: LiveData<MultiLiveEvent.Event>,
    event: LiveData<MultiLiveEvent.Event>,
    blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher
) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(event, navController, lifecycleOwner) {
        val activityObserver = Observer { event: MultiLiveEvent.Event ->
            when (event) {
                is MainActivityViewModel.LaunchBlazeCampaignCreation -> coroutineScope.launch {
                    blazeCampaignCreationDispatcher.startCampaignCreation(
                        source = BlazeFlowSource.LOCAL_NOTIFICATION_NO_CAMPAIGN_REMINDER
                    )
                }
            }
        }

        val observer = Observer { event: MultiLiveEvent.Event ->
            when (event) {
                is DashboardBlazeViewModel.LaunchBlazeCampaignCreation -> coroutineScope.launch {
                    blazeCampaignCreationDispatcher.startCampaignCreation(
                        source = BlazeFlowSource.MY_STORE_SECTION,
                        productId = event.productId
                    )
                }

                is DashboardBlazeViewModel.ShowAllCampaigns -> {
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToBlazeCampaignListFragment()
                    )
                }

                is DashboardBlazeViewModel.ShowCampaignDetails -> {
                    navController.navigateSafely(
                        NavGraphMainDirections.actionGlobalBlazeCampaignDetailWebViewFragment(
                            campaignId = event.campaignId
                        )
                    )
                }
            }
        }

        activityEvent.observe(lifecycleOwner, activityObserver)
        event.observe(lifecycleOwner, observer)

        onDispose {
            event.removeObserver(activityObserver)
            event.removeObserver(observer)
        }
    }
}

@Composable
private fun BlazeFrame(
    modifier: Modifier,
    state: DashboardBlazeCampaignState,
    onContactSupportClicked: () -> Unit,
    onRetryOnErrorButtonClicked: () -> Unit,
    content: @Composable () -> Unit
) {
    WidgetCard(
        modifier = modifier,
        titleResource = DashboardWidget.Type.BLAZE.titleResource,
        iconResource = R.drawable.ic_blaze,
        menu = state.menu,
        button = state.mainButton,
        isError = state is DashboardBlazeCampaignState.Error
    ) {
        if (state is DashboardBlazeCampaignState.Error) {
            WidgetError(
                onContactSupportClicked = onContactSupportClicked,
                onRetryClicked = onRetryOnErrorButtonClicked
            )
        } else {
            content()
        }
    }
}

@Composable
fun DashboardBlazeView(
    state: DashboardBlazeCampaignState,
    modifier: Modifier = Modifier,
    onContactSupportClicked: () -> Unit = {},
    onRetryOnErrorButtonClicked: () -> Unit = {}
) {
    BlazeFrame(modifier, state, onContactSupportClicked, onRetryOnErrorButtonClicked) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                )
        ) {
            when (state) {
                is DashboardBlazeCampaignState.Loading -> BlazeCampaignLoading(
                    modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.major_100))
                )

                is DashboardBlazeCampaignState.Campaign -> {
                    BlazeCampaignItem(
                        campaign = state.campaign,
                        onCampaignClicked = state.onCampaignClicked,
                    )

                    WooOutlinedButton(
                        text = stringResource(string.blaze_campaign_create_campaign_button),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(id = R.dimen.minor_100)),
                        onClick = state.onCreateCampaignClicked,
                    )
                }

                is DashboardBlazeCampaignState.NoCampaign -> {
                    Text(
                        modifier = Modifier.padding(
                            end = dimensionResource(id = R.dimen.major_300)
                        ),
                        text = stringResource(id = string.blaze_campaign_subtitle),
                        style = WooTheme.text.bodyLarge.regular,
                        color = WooTheme.colors.surface.onDefault,
                    )
                    BlazeProductItem(
                        product = state.product,
                        onProductSelected = state.onProductClicked,
                        modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
                    )

                    WooOutlinedButton(
                        text = stringResource(string.blaze_campaign_create_campaign_button),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = dimensionResource(id = R.dimen.minor_100),
                                bottom = dimensionResource(id = R.dimen.major_100)
                            ),
                        onClick = state.onCreateCampaignClicked,
                    )
                }

                else -> error("Invalid state")
            }
        }
    }
}

@Composable
fun BlazeProductItem(
    product: BlazeProductUi,
    onProductSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(
                width = WooTheme.stroke.regular,
                color = WooTheme.colors.outlineVariant,
                shape = RoundedCornerShape(WooTheme.radius.medium),
            )
            .clip(shape = RoundedCornerShape(WooTheme.radius.medium))
            .clickable { onProductSelected() }
            .padding(WooTheme.padding.padding5)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductThumbnail(imageUrl = product.imgUrl)
            Column(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f)
            ) {
                Text(
                    modifier = Modifier
                        .padding(bottom = dimensionResource(id = R.dimen.minor_50)),
                    text = stringResource(id = R.string.blaze_campaign_suggested_product_caption),
                    style = WooTheme.text.bodySmall.regular,
                    color = WooTheme.colors.surface.onDefault,
                )
                Text(
                    text = product.name,
                    style = WooTheme.text.titleMedium.strong,
                    color = WooTheme.colors.surface.onDefault,
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = WooTheme.colors.surface.onVariant,
            )
        }
    }
}

@Composable
private fun BlazeCampaignLoading(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = WooTheme.stroke.regular,
                color = WooTheme.colors.outlineVariant,
                shape = RoundedCornerShape(WooTheme.radius.medium),
            )
            .clip(shape = RoundedCornerShape(WooTheme.radius.medium))
            .padding(WooTheme.padding.padding5)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardSkeleton(
                    width = dimensionResource(id = R.dimen.major_275),
                    height = dimensionResource(id = R.dimen.major_275)
                )

                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))

                DashboardSkeleton(
                    width = dimensionResource(id = R.dimen.skeleton_text_large_width),
                    height = dimensionResource(id = R.dimen.skeleton_text_height_100),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_275)))
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))
                DashboardSkeleton(
                    width = dimensionResource(id = R.dimen.skeleton_text_medium_width),
                    height = dimensionResource(id = R.dimen.skeleton_text_height_100),
                )
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))

                DashboardSkeleton(
                    width = dimensionResource(id = R.dimen.skeleton_text_medium_width),
                    height = dimensionResource(id = R.dimen.skeleton_text_height_100),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun MyStoreBlazeViewCampaignPreview() =
    WooDesignSystemThemeWithBackground { MyStoreBlazeViewCampaignPreviewContent() }

@Composable
private fun MyStoreBlazeViewCampaignPreviewContent() {
    val product = BlazeProductUi(
        name = "Product name",
        imgUrl = "",
    )
    DashboardBlazeView(
        state = DashboardBlazeCampaignState.Campaign(
            campaign = BlazeCampaignUi(
                product = product,
                status = CampaignStatusUi.Active,
                isEndlessCampaign = false,
                impressions = "32435",
                clicks = "43",
                formattedBudget = "$100",
                budgetLabel = R.string.blaze_campaign_status_budget_total
            ),
            onCampaignClicked = {},
            onCreateCampaignClicked = {},
            menu = DashboardWidgetMenu(
                items = listOf(
                    DashboardWidget.Type.BLAZE.defaultHideMenuEntry(
                        onHideClicked = { }
                    )
                )
            ),
            showAllCampaignsButton = DashboardWidgetAction(
                titleResource = R.string.blaze_campaign_show_all_button,
                action = {}
            ),
        ),
        onContactSupportClicked = {},
        onRetryOnErrorButtonClicked = {},
    )
}

@PreviewLightDark
@Composable
fun MyStoreBlazeViewNoCampaignPreview() =
    WooDesignSystemThemeWithBackground { MyStoreBlazeViewNoCampaignPreviewContent() }

@Composable
private fun MyStoreBlazeViewNoCampaignPreviewContent() {
    DashboardBlazeView(
        state = DashboardBlazeCampaignState.NoCampaign(
            product = BlazeProductUi(
                name = "Product name",
                imgUrl = "",
            ),
            onProductClicked = {},
            menu = DashboardWidgetMenu(
                items = listOf(
                    DashboardWidget.Type.BLAZE.defaultHideMenuEntry(
                        onHideClicked = { }
                    )
                )
            ),
            onCreateCampaignClicked = {},
        ),
        onContactSupportClicked = {},
        onRetryOnErrorButtonClicked = {},
    )
}

@PreviewLightDark
@Composable
fun DashboardBlazeLoadingPreview() {
    WooDesignSystemThemeWithBackground {
        DashboardBlazeView(
            state = DashboardBlazeCampaignState.Loading,
            onContactSupportClicked = {},
            onRetryOnErrorButtonClicked = {},
        )
    }
}
