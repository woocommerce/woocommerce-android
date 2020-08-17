package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductPricingViewModel.ExitWithResult
import com.woocommerce.android.ui.products.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.ProductPricingViewModel.ViewState
import com.woocommerce.android.ui.products.ProductTaxStatus.Taxable
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date

@ExperimentalCoroutinesApi
class ProductPricingViewModelTest : BaseUnitTest() {
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val productRepository: ProductDetailRepository = mock()

    private val pricingData = PricingData(
        taxClass = "standard",
        taxStatus = Taxable,
        isSaleScheduled = true,
        saleStartDate = Date(),
        saleEndDate = Date(),
        regularPrice = BigDecimal(10),
        salePrice = BigDecimal.ONE
    )

    private val savedState: SavedStateWithArgs = spy(
        SavedStateWithArgs(
            SavedStateHandle(),
            null,
            ProductPricingFragmentArgs(RequestCodes.PRODUCT_DETAIL_PRICING, pricingData)
        )
    )

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private lateinit var viewModel: ProductPricingViewModel

    private val taxClasses = listOf(
        TaxClass("standard"),
        TaxClass("weird")
    )
    private val viewState = ViewState(
        "$",
        2,
        taxClasses,
        null,
        null,
        pricingData,
        true
    )

    @Before
    fun setup() {
        val siteSettings = mock<WCSettingsModel> {
            on(it.currencyCode).thenReturn(viewState.currency)
            on(it.currencyDecimalNumber).thenReturn(viewState.decimals)
        }

        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(siteSettings).whenever(wooCommerceStore).getSiteSettings(any())
        doReturn(taxClasses).whenever(productRepository).getTaxClassesForSite()

        viewModel = spy(ProductPricingViewModel(
            savedState,
            coroutinesTestRule.testDispatchers,
            productRepository,
            wooCommerceStore,
            selectedSite
        ))

        clearInvocations(
            savedState,
            productRepository,
            wooCommerceStore,
            selectedSite
        )
    }

    @Test
    fun `Displays the initial price information correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var state: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }

        assertThat(state).isEqualTo(viewState)
    }

    @Test
    fun `Displays and hides the done button after data change`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var state: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }

        val newPrice = BigDecimal(4)
        viewModel.onRegularPriceEntered(newPrice)

        val expectedState = viewState.copy(
            pricingData = pricingData.copy(
                regularPrice = newPrice
            ),
            isDoneButtonVisible = true,
            salePriceErrorMessage = 0
        )

        assertThat(state).isEqualTo(expectedState)

        viewModel.onRegularPriceEntered(viewState.pricingData.regularPrice!!)

        val restoredState = viewState.copy(
            pricingData = pricingData.copy(
                regularPrice = viewState.pricingData.regularPrice
            ),
            isDoneButtonVisible = false,
            salePriceErrorMessage = 0
        )
        assertThat(state).isEqualTo(restoredState)
    }

    @Test
    fun `Disables the done button if there is validation error`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var state: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }

        val newPrice = BigDecimal(14)
        viewModel.onSalePriceEntered(newPrice)

        val expectedState = viewState.copy(
            pricingData = pricingData.copy(
                salePrice = newPrice
            ),
            isDoneButtonVisible = true,
            salePriceErrorMessage = R.string.product_pricing_update_sale_price_error
        )

        assertThat(state).isEqualTo(expectedState)
        assertThat(state?.isDoneButtonEnabled).isFalse()
    }

    @Test
    fun `Hides the tax section for variation pricing`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val savedState: SavedStateWithArgs = spy(
            SavedStateWithArgs(
                SavedStateHandle(),
                null,
                ProductPricingFragmentArgs(RequestCodes.VARIATION_DETAIL_PRICING, pricingData)
            )
        )

        viewModel = spy(ProductPricingViewModel(
            savedState,
            coroutinesTestRule.testDispatchers,
            productRepository,
            wooCommerceStore,
            selectedSite
        ))

        var state: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }

        val expectedState = viewState.copy(
            taxClassList = null,
            isTaxSectionVisible = false
        )

        assertThat(state).isEqualTo(expectedState)
    }

    @Test
    fun `Makes sale end date equal to start date if earlier`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var state: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }

        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DATE, 1)
        val startDate = calendar.time

        viewModel.onScheduledSaleChanged(true)
        viewModel.onDataChanged(
            saleStartDate = startDate,
            saleEndDate = endDate
        )

        val expectedState = viewState.copy(
            isDoneButtonVisible = true,
            pricingData = pricingData.copy(
                isSaleScheduled = true,
                saleStartDate = startDate,
                saleEndDate = startDate
            )
        )

        assertThat(state).isEqualTo(expectedState)
    }

    @Test
    fun `Nulls the sale schedule dates if switch off`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var state: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }

        var firedEvent: Event? = null
        viewModel.event.observeForever {
            firedEvent = it
        }

        val calendar = Calendar.getInstance()
        val startDate = calendar.time
        calendar.add(Calendar.DATE, 1)
        val endDate = calendar.time

        viewModel.onScheduledSaleChanged(true)
        viewModel.onDataChanged(
            saleStartDate = startDate,
            saleEndDate = endDate
        )

        val expectedState = viewState.copy(
            isDoneButtonVisible = true,
            pricingData = pricingData.copy(
                isSaleScheduled = true,
                saleStartDate = startDate,
                saleEndDate = endDate
            )
        )

        assertThat(state).isEqualTo(expectedState)

        viewModel.onScheduledSaleChanged(false)

        val resetState = viewState.copy(
            isDoneButtonVisible = true,
            pricingData = pricingData.copy(
                isSaleScheduled = false,
                saleStartDate = startDate,
                saleEndDate = endDate
            )
        )

        assertThat(state).isEqualTo(resetState)

        viewModel.onDoneButtonClicked()

        val expectedResultData = resetState.pricingData.copy(
            saleStartDate = null,
            saleEndDate = null
        )

        assertThat((firedEvent as? ExitWithResult)?.data).isEqualTo(expectedResultData)
    }
}
