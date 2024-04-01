package com.woocommerce.android.ui.blaze

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.MyStoreBlazeViewModel.MyStoreBlazeCampaignState
import com.woocommerce.android.ui.blaze.campaigs.BlazeCampaignItem
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.WCOverflowMenu
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.mystore.MyStoreFragmentDirections
import kotlinx.coroutines.launch

@Composable
fun MyStoreBlazeView(
    blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher,
    onHidden: () -> Unit,
    viewModel: MyStoreBlazeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val lifecycle = LocalLifecycleOwner.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    viewModel.blazeViewState.observeAsState().value?.let {
        val isHidden = it is MyStoreBlazeCampaignState.Hidden

        LaunchedEffect(isHidden) {
            if (isHidden) {
                onHidden()
            }
        }

        if (!isHidden) {
            MyStoreBlazeView(
                state = it,
                onDismissBlazeView = viewModel::onBlazeViewDismissed
            )
        }
    }

    // This is not needed now since we attach the fragment in the BlazeCampaignCreationDispatcher, but it's just a sample
    // of we can use this on difference scenarios if needed
//    HandleResult<Collection<SelectedItem>>(key = ProductSelectorFragment.PRODUCT_SELECTOR_RESULT) { items ->
//        with(blazeCampaignCreationDispatcher) {
//            view.findNavController().navigateToBlazeGraph(
//                startDestination = R.id.blazeCampaignCreationPreviewFragment,
//                bundle = BlazeCampaignCreationPreviewFragmentArgs(
//                    productId = items.first().id,
//                    source = BlazeFlowSource.MY_STORE_SECTION
//                ).toBundle()
//            )
//        }
//    }

    LaunchedEffect(viewModel) {
        viewModel.event.observe(lifecycle) { event ->
            when (event) {
                is MyStoreBlazeViewModel.LaunchBlazeCampaignCreationUsingWebView -> view.findNavController()
                    .navigateSafely(
                        NavGraphMainDirections.actionGlobalBlazeCampaignCreationFragment(
                            urlToLoad = event.url,
                            source = event.source
                        )
                    )

                is MyStoreBlazeViewModel.LaunchBlazeCampaignCreation -> coroutineScope.launch {
                    blazeCampaignCreationDispatcher.startCampaignCreation(
                        source = BlazeFlowSource.MY_STORE_SECTION,
                        productId = event.productId
                    )
                }

                is MyStoreBlazeViewModel.ShowAllCampaigns -> {
                    view.findNavController().navigateSafely(
                        MyStoreFragmentDirections.actionMyStoreToBlazeCampaignListFragment()
                    )
                }

                is MyStoreBlazeViewModel.ShowCampaignDetails -> {
                    view.findNavController().navigateSafely(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                            urlToLoad = event.url,
                            urlsToTriggerExit = arrayOf(event.urlToTriggerExit),
                            title = view.resources.getString(R.string.blaze_campaign_details_title)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MyStoreBlazeView(
    state: MyStoreBlazeCampaignState,
    onDismissBlazeView: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Column(
                    modifier = Modifier.padding(
                        top = dimensionResource(id = R.dimen.major_100),
                        start = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_100),
                    )
                ) {
                    BlazeCampaignHeader()
                    when (state) {
                        is MyStoreBlazeCampaignState.Campaign -> BlazeCampaignItem(
                            campaign = state.campaign,
                            onCampaignClicked = state.onCampaignClicked,
                            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
                        )

                        is MyStoreBlazeCampaignState.NoCampaign -> {
                            Text(
                                modifier = Modifier.padding(
                                    top = dimensionResource(id = R.dimen.major_100),
                                    end = dimensionResource(id = R.dimen.major_300)
                                ),
                                text = stringResource(id = R.string.blaze_campaign_subtitle),
                                style = MaterialTheme.typography.body1,
                            )
                            BlazeProductItem(
                                product = state.product,
                                onProductSelected = state.onProductClicked,
                                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
                            )
                        }

                        else -> error("Invalid state")
                    }
                }
                when (state) {
                    is MyStoreBlazeCampaignState.Campaign -> ShowAllOrCreateCampaignFooter(
                        onShowAllClicked = state.onViewAllCampaignsClicked,
                        onCreateCampaignClicked = state.onCreateCampaignClicked
                    )

                    is MyStoreBlazeCampaignState.NoCampaign -> CreateCampaignFooter(
                        onCreateCampaignClicked = state.onCreateCampaignClicked,
                        modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
                    )

                    else -> error("Invalid state")
                }
            }
            WCOverflowMenu(
                items = arrayOf(stringResource(id = R.string.blaze_overflow_menu_hide_blaze)),
                onSelected = { onDismissBlazeView() },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun CreateCampaignFooter(
    onCreateCampaignClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Divider(Modifier.padding(start = dimensionResource(id = R.dimen.major_100)))
        WCTextButton(
            onClick = onCreateCampaignClicked,
            contentPadding = PaddingValues(
                start = dimensionResource(id = R.dimen.major_100),
                end = dimensionResource(id = R.dimen.major_100),
                top = ButtonDefaults.TextButtonContentPadding.calculateTopPadding(),
                bottom = ButtonDefaults.TextButtonContentPadding.calculateBottomPadding(),
            )
        ) {
            Text(stringResource(id = R.string.blaze_campaign_create_campaign_button))
        }
    }
}

@Composable
private fun ShowAllOrCreateCampaignFooter(
    onShowAllClicked: () -> Unit,
    onCreateCampaignClicked: () -> Unit,
) {
    Row {
        WCTextButton(
            modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_75)),
            onClick = onShowAllClicked
        ) {
            Text(stringResource(id = R.string.blaze_campaign_show_all_button))
        }
        WCTextButton(
            modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100)),
            onClick = onCreateCampaignClicked
        ) {
            Text(stringResource(id = R.string.blaze_campaign_create_campaign_button))
        }
    }
}

@Composable
private fun BlazeCampaignHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = R.drawable.ic_blaze),
            contentDescription = "", // Blaze icon, no relevant content desc
            modifier = Modifier
                .padding(end = dimensionResource(id = R.dimen.minor_100))
                .size(dimensionResource(id = R.dimen.major_125))
        )
        Text(
            text = stringResource(id = R.string.blaze_campaign_title),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold
        )
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
                width = dimensionResource(id = R.dimen.minor_10),
                color = colorResource(R.color.divider_color),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
            )
            .clip(shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
            .clickable { onProductSelected() }
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductThumbnail(imageUrl = product.imgUrl)
            Text(
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.major_100))
                    .weight(1f),
                text = product.name,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null
            )
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
fun MyStoreBlazeViewCampaignPreview() {
    val product = BlazeProductUi(
        name = "Product name",
        imgUrl = "",
    )
    MyStoreBlazeView(
        state = MyStoreBlazeCampaignState.Campaign(
            campaign = BlazeCampaignUi(
                product = product,
                status = CampaignStatusUi.Active,
                stats = listOf(
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_impressions,
                        value = 100.toString()
                    ),
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_clicks,
                        value = 10.toString()
                    ),
                    BlazeCampaignStat(
                        name = R.string.blaze_campaign_status_budget,
                        value = 1000.toString()
                    ),
                ),
            ),
            onCampaignClicked = {},
            onViewAllCampaignsClicked = {},
            onCreateCampaignClicked = {}
        ),
        onDismissBlazeView = {}
    )
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun MyStoreBlazeViewNoCampaignPreview() {
    MyStoreBlazeView(
        state = MyStoreBlazeCampaignState.NoCampaign(
            product = BlazeProductUi(
                name = "Product name",
                imgUrl = "",
            ),
            onProductClicked = {},
            onCreateCampaignClicked = {}
        ),
        onDismissBlazeView = {}
    )
}
