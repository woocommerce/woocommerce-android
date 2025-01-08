package com.woocommerce.android.ui.woopos.home.items.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosItemsScreenViewModel @Inject constructor(
    private val navigator: WooPosItemsNavigator,
) : ViewModel() {
    private val _screenState = MutableStateFlow<ItemsScreens>(ItemsScreens.ItemListScreen)
    val screenState: StateFlow<ItemsScreens> = _screenState

    init {
        viewModelScope.launch {
            listenToNavigationEvents()
        }
    }

    private fun navigateToVariationsScreen(product: VariableProductData) {
        _screenState.value = ItemsScreens.VariationsScreen(product)
    }

    private fun navigateBackToItemListScreen() {
        _screenState.value = ItemsScreens.ItemListScreen
    }

    private suspend fun listenToNavigationEvents() {
        navigator.events.collect {
            when (it) {
                is WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen -> {
                    navigateToVariationsScreen(it.product)
                }
                is WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen -> {
                    navigateBackToItemListScreen()
                }
            }
        }
    }

    fun onUiEvent(wooPosLeftPaneScreensNavigationEvent: WooPosItemsNavigator.WooPosItemsScreenNavigationEvent) {
        viewModelScope.launch {
            navigator.sendNavigationEvent(wooPosLeftPaneScreensNavigationEvent)
        }
    }

    sealed class ItemsScreens {
        data object ItemListScreen : ItemsScreens()
        data class VariationsScreen(val product: VariableProductData) : ItemsScreens()
    }
}
