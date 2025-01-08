package com.woocommerce.android.ui.woopos.home.items.variations

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.Button
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.items.ItemsLoadingIndicator
import com.woocommerce.android.ui.woopos.home.items.WooPosItem
import com.woocommerce.android.ui.woopos.home.items.WooPosItemList
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import com.woocommerce.android.ui.woopos.home.items.WooPosVariationsViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WooPosVariationsScreen(
    modifier: Modifier,
    variableProductData: VariableProductData,
    onBackClicked: () -> Unit
) {
    val viewModel: WooPosVariationsViewModel = hiltViewModel(
        key = variableProductData.id.toString()
    )
    LaunchedEffect(variableProductData.id) {
        viewModel.init(variableProductData.id)
    }
    val state = viewModel.viewState
    WooPosVariationsScreens(
        modifier,
        onBackClicked,
        onItemClicked = { productId, variationId ->
            viewModel.onUIEvent(WooPosVariationsUIEvents.OnItemClicked(productId, variationId))
        },
        onEndOfItemListReached = {
            viewModel.onUIEvent(
                WooPosVariationsUIEvents.EndOfItemsListReached(
                    variableProductData.id,
                    variableProductData.numOfVariations
                )
            )
        },
        onPullToRefresh = {
            viewModel.onUIEvent(WooPosVariationsUIEvents.PullToRefreshTriggered(variableProductData.id))
        },
        onRetryClicked = {
            viewModel.onUIEvent(
                WooPosVariationsUIEvents.VariationsLoadingErrorRetryButtonClicked(variableProductData.id)
            )
        },
        variableProductData,
        state,
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WooPosVariationsScreens(
    modifier: Modifier,
    onBackClicked: () -> Unit,
    onItemClicked: (Long, Long) -> Unit,
    onEndOfItemListReached: () -> Unit,
    onPullToRefresh: () -> Unit,
    onRetryClicked: () -> Unit,
    variableProductData: VariableProductData,
    state: StateFlow<WooPosVariationsViewState>,
) {
    val itemState = state.collectAsState()
    val pullToRefreshState = rememberPullRefreshState(
        itemState.value.reloadingProductsWithPullToRefresh,
        onPullToRefresh
    )
    val listState = rememberLazyListState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullToRefreshState)
            .padding(
                start = 16.dp.toAdaptivePadding(),
                end = 16.dp.toAdaptivePadding(),
                top = 32.dp.toAdaptivePadding(),
                bottom = 0.dp.toAdaptivePadding(),
            )
    ) {
        BackHandler(onBack = onBackClicked)
        Column(
            modifier = modifier.fillMaxHeight()
        ) {
            VariationsToolbar(
                variableProductData = variableProductData,
                onBackClicked = onBackClicked
            )
            when (val itemsState = itemState.value) {
                is WooPosVariationsViewState.Content -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    WooPosItemList(
                        state = itemsState,
                        listState = listState,
                        onItemClicked = {
                            onItemClicked(
                                (it as WooPosItem.Variation).productId,
                                it.id
                            )
                        },
                        onEndOfProductsListReached = onEndOfItemListReached,
                        onErrorWhilePaginating = {
                            VariationsPaginationError {
                                onEndOfItemListReached()
                            }
                        }
                    )
                }

                is WooPosVariationsViewState.Loading -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    ItemsLoadingIndicator()
                }

                is WooPosVariationsViewState.Error -> {
                    VariationsError(modifier = Modifier.width(640.dp)) {
                        onRetryClicked()
                    }
                }

                else -> {}
            }
        }
        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = itemState.value.reloadingProductsWithPullToRefresh,
            state = pullToRefreshState
        )
    }
}

@Composable
fun VariationsError(modifier: Modifier, onRetryClicked: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        WooPosErrorScreen(
            modifier = modifier,
            message = stringResource(id = R.string.woopos_variations_loading_error_title),
            reason = stringResource(id = R.string.woopos_products_loading_error_message),
            primaryButton = Button(
                text = stringResource(id = R.string.woopos_products_loading_error_retry_button),
                click = onRetryClicked
            )
        )
    }
}

@Composable
fun VariationsPaginationError(onRetryClicked: () -> Unit) {
    WooPosPaginationErrorIndicator(
        message = stringResource(id = R.string.woopos_items_pagination_error_title),
        description = stringResource(id = R.string.woopos_items_pagination_error_description),
        primaryButton = Button(
            text = stringResource(id = R.string.woopos_items_pagination_try_again_label),
            click = onRetryClicked
        ),
    )
}

@Composable
private fun VariationsToolbar(
    variableProductData: VariableProductData,
    onBackClicked: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val (backButton, productName, variationsCount) = createRefs()

        IconButton(
            onClick = onBackClicked,
            modifier = Modifier.constrainAs(backButton) {
                start.linkTo(parent.start)
                top.linkTo(productName.top)
                bottom.linkTo(productName.bottom)
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.woopos_variations_back_content_description),
                tint = MaterialTheme.colors.onSurface
            )
        }

        Text(
            text = variableProductData.name,
            style = MaterialTheme.typography.h4.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.constrainAs(productName) {
                start.linkTo(backButton.end, margin = 8.dp)
                top.linkTo(parent.top, margin = 8.dp)
            }
        )

        Text(
            text = stringResource(
                id = R.string.woopos_items_list_variable_product_variations,
                variableProductData.numOfVariations
            ),
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.constrainAs(variationsCount) {
                start.linkTo(productName.start)
                top.linkTo(productName.bottom, margin = 4.dp)
            }
        )
    }
}

@Composable
@WooPosPreview
fun WooPosVariationsScreenPreview() {
    val productState = MutableStateFlow(
        WooPosVariationsViewState.Content(
            items = listOf(
                WooPosItem.Variation(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    productId = 1,
                    price = "10.0$",
                    imageUrl = null,
                ),
                WooPosItem.Variation(
                    2,
                    name = "Product 2",
                    productId = 1,
                    price = "2000.00$",
                    imageUrl = null,
                ),
                WooPosItem.Variation(
                    3,
                    name = "Product 3",
                    productId = 1,
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            reloadingProductsWithPullToRefresh = true,
        )
    )
    WooPosTheme {
        WooPosVariationsScreens(
            modifier = Modifier,
            onBackClicked = {},
            onItemClicked = { _, _ -> },
            onEndOfItemListReached = {},
            onPullToRefresh = {},
            onRetryClicked = {},
            variableProductData = VariableProductData(
                id = 0,
                name = "Variable Product",
                numOfVariations = 20,
            ),
            state = productState,
        )
    }
}
