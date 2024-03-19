package com.woocommerce.android.ui.products.variations.selector

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.selector.ProductSelectorTracker
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.ui.products.selector.ProductSourceForTracking
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.VerificationCollector
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class VariationSelectorViewModelTest : BaseUnitTest() {
    @Rule @JvmField
    val verificationCollector: VerificationCollector = MockitoJUnit.collector()

    private val variationSelectorRepository: VariationSelectorRepository = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val listHandler: VariationListHandler = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val productSelectorTracker: ProductSelectorTracker = ProductSelectorTracker(tracker)

    @Test
    fun `given order creation flow, when item is unselected, should track analytic event`() = testBlocking {
        val navArgs = VariationSelectorFragmentArgs(
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            productId = 1L,
            variationIds = longArrayOf(2L, 3L),
            productSource = ProductSourceForTracking.ALPHABETICAL,
            screenMode = VariationSelectorViewModel.ScreenMode.FULLSCREEN
        ).toSavedStateHandle()

        val sut = createViewModel(navArgs)
        val listItem = VariationSelectorViewModel.VariationListItem(1, "")
        sut.onVariationClick(listItem) // select
        sut.onVariationClick(listItem) // unselect

        verify(tracker).track(AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_ITEM_UNSELECTED)
    }

    @Test
    fun `given order creation flow, when item is selected, should track analytic event`() = testBlocking {
        val navArgs = VariationSelectorFragmentArgs(
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            productId = 1L,
            variationIds = longArrayOf(2L, 3L),
            productSource = ProductSourceForTracking.ALPHABETICAL,
            screenMode = VariationSelectorViewModel.ScreenMode.FULLSCREEN
        ).toSavedStateHandle()

        val sut = createViewModel(navArgs)
        val listItem = VariationSelectorViewModel.VariationListItem(1, "")
        sut.onVariationClick(listItem)

        verify(tracker).track(AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_ITEM_SELECTED)
    }

    @Test
    fun `given order creation flow, when clear button is tapped, should track analytics event`() = testBlocking {
        val navArgs = VariationSelectorFragmentArgs(
            productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.OrderCreation,
            productId = 1L,
            variationIds = longArrayOf(2L, 3L),
            productSource = ProductSourceForTracking.ALPHABETICAL,
            screenMode = VariationSelectorViewModel.ScreenMode.FULLSCREEN
        ).toSavedStateHandle()

        val sut = createViewModel(navArgs)
        sut.onClearButtonClick()

        verify(tracker).track(
            AnalyticsEvent.ORDER_CREATION_PRODUCT_SELECTOR_CLEAR_SELECTION_BUTTON_TAPPED,
            mapOf("source" to "variation_selector")
        )
    }

    private fun createViewModel(navArgs: SavedStateHandle): VariationSelectorViewModel {
        return VariationSelectorViewModel(
            navArgs,
            variationSelectorRepository,
            currencyFormatter,
            wooCommerceStore,
            selectedSite,
            listHandler,
            resourceProvider,
            productSelectorTracker
        )
    }
}
