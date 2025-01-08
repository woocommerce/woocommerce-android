package com.woocommerce.android.ui.woopos.home.items.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsScreen
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsScreen
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WooPosItemsScreens(modifier: Modifier) {
    val viewModel: WooPosItemsScreenViewModel = hiltViewModel()
    WooPosItemsScreens(modifier = modifier, itemsScreens = viewModel.screenState) {
        viewModel.onUiEvent(WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen)
    }
}

@Composable
fun WooPosItemsScreens(
    modifier: Modifier,
    itemsScreens: StateFlow<WooPosItemsScreenViewModel.ItemsScreens>,
    onNavigateToItemsListScreen: () -> Unit
) {
    val currentNavigationState = itemsScreens.collectAsState()
    val listState = rememberLazyListState()
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(targetState = currentNavigationState.value, label = "LeftPaneScreen") { navigationState ->
            when (navigationState) {
                is WooPosItemsScreenViewModel.ItemsScreens.ItemListScreen -> {
                    WooPosItemsScreen(modifier = modifier, listState)
                }

                is WooPosItemsScreenViewModel.ItemsScreens.VariationsScreen -> {
                    NavigateToVariationsScreen(
                        variableProductData = navigationState.product,
                        modifier = modifier,
                        onBackClicked = { onNavigateToItemsListScreen() }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigateToVariationsScreen(
    variableProductData: VariableProductData,
    modifier: Modifier,
    onBackClicked: () -> Unit,
) {
    WooPosVariationsScreen(
        modifier,
        variableProductData,
        onBackClicked = onBackClicked
    )
}
