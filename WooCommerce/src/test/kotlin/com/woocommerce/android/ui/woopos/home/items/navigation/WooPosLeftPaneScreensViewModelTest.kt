package com.woocommerce.android.ui.woopos.home.items.navigation

import com.woocommerce.android.ui.woopos.home.items.WooPosItemNavigationData.VariableProductData
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosLeftPaneScreensViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private lateinit var navigator: WooPosItemsNavigator
    private lateinit var viewModel: WooPosItemsScreenViewModel
    private val navigationEvents = MutableSharedFlow<WooPosItemsNavigator.WooPosItemsScreenNavigationEvent>()

    @Before
    fun setUp() {
        navigator = mock {
            on { events } doReturn navigationEvents
        }
        viewModel = WooPosItemsScreenViewModel(navigator)
    }

    @Test
    fun `given view model init, then initial state is ItemListScreen`() = runTest {
        assert(viewModel.screenState.value is WooPosItemsScreenViewModel.ItemsScreens.ItemListScreen)
    }

    @Test
    fun `given navigation event is NavigateToVariationsScreen, then updates screen state`() = runTest {
        val product = VariableProductData(
            1L,
            "Product Name",
            numOfVariations = 10,
        )
        navigationEvents.emit(WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen(product))

        assert(viewModel.screenState.value is WooPosItemsScreenViewModel.ItemsScreens.VariationsScreen)
        assert(
            (
                viewModel.screenState.value as WooPosItemsScreenViewModel.ItemsScreens.VariationsScreen
                ).product == product
        )
    }

    @Test
    fun `given navigate back to ItemListScreen then updates screen state`() = runTest {
        val product = VariableProductData(
            1L,
            "Product Name",
            numOfVariations = 10,
        )
        navigationEvents.emit(WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen(product))
        navigationEvents.emit(WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen)

        assert(viewModel.screenState.value is WooPosItemsScreenViewModel.ItemsScreens.ItemListScreen)
    }

    @Test
    fun `given view model init, then listen to navigation events and updates screen state`() = runTest {
        val product = VariableProductData(
            1L,
            "Product Name",
            numOfVariations = 10,
        )
        navigationEvents.emit(WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateToVariationsScreen(product))

        assert(viewModel.screenState.value is WooPosItemsScreenViewModel.ItemsScreens.VariationsScreen)
        assert(
            (
                viewModel.screenState.value as WooPosItemsScreenViewModel.ItemsScreens.VariationsScreen
                ).product == product
        )

        navigationEvents.emit(WooPosItemsNavigator.WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen)

        assert(viewModel.screenState.value is WooPosItemsScreenViewModel.ItemsScreens.ItemListScreen)
    }
}
