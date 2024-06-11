package com.woocommerce.android.ui.products.price

import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductTaxStatus.Taxable
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.models.CurrencyFormattingParameters
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.PricingData
import com.woocommerce.android.ui.products.price.ProductPricingViewModel.ViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date

@ExperimentalCoroutinesApi
class ProductPricingViewModelTest : BaseUnitTest() {
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val productRepository: ProductDetailRepository = mock()

    private val siteParams = SiteParameters(
        currencyCode = "USD",
        currencySymbol = "$",
        currencyFormattingParameters = CurrencyFormattingParameters(
            "",
            "",
            2,
            LEFT
        ),
        weightUnit = "kg",
        dimensionUnit = "cm",
        gmtOffset = 0f
    )
    private val parameterRepository: ParameterRepository = mock {
        on(it.getParameters(any(), any())).thenReturn(siteParams)
    }

    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private val pricingData = PricingData(
        taxClass = "standard",
        taxStatus = Taxable,
        isSaleScheduled = true,
        saleStartDate = Date(),
        saleEndDate = Date(),
        regularPrice = BigDecimal(10),
        salePrice = BigDecimal.ONE
    )

    private val savedState = ProductPricingFragmentArgs(RequestCodes.PRODUCT_DETAIL_PRICING, pricingData)
        .toSavedStateHandle()

    private lateinit var viewModel: ProductPricingViewModel

    private val taxClasses = listOf(
        TaxClass("standard"),
        TaxClass("weird")
    )
    private val viewState = ViewState(
        currency = siteParams.currencySymbol,
        currencyPosition = siteParams.currencyFormattingParameters?.currencyPosition,
        decimals = 2,
        taxClassList = taxClasses,
        salePriceErrorMessage = null,
        pricingData = pricingData,
        isTaxSectionVisible = true
    )

    @Before
    fun setup() {
        val siteSettings = mock<WCSettingsModel> {
            on(it.currencyDecimalNumber).thenReturn(viewState.decimals)
        }
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(siteSettings).whenever(wooCommerceStore).getSiteSettings(any())
        doReturn(taxClasses).whenever(productRepository).getTaxClassesForSite()

        viewModel = ProductPricingViewModel(
            savedState,
            productRepository,
            wooCommerceStore,
            selectedSite,
            parameterRepository,
            analyticsTracker,
        )

        clearInvocations(
            productRepository,
            wooCommerceStore,
            selectedSite
        )
    }

    @Test
    fun `Displays the initial price information correctly`() = testBlocking {
        var state: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }

        assertThat(state).isEqualTo(viewState)
    }

    @Test
    fun `Displays and hides the done button after data change`() = testBlocking {
        var state: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }

        val newPrice = BigDecimal(4)
        viewModel.onRegularPriceEntered(newPrice)

        val expectedState = viewState.copy(
            pricingData = pricingData.copy(
                regularPrice = newPrice
            ),
            salePriceErrorMessage = 0
        )

        assertThat(state).isEqualTo(expectedState)

        viewModel.onRegularPriceEntered(viewState.pricingData.regularPrice!!)

        val restoredState = viewState.copy(
            pricingData = pricingData.copy(
                regularPrice = viewState.pricingData.regularPrice
            ),
            salePriceErrorMessage = 0
        )
        assertThat(state).isEqualTo(restoredState)
    }

    @Test
    fun `Displays error message if there is validation error`() = testBlocking {
        var state: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }

        val newPrice = BigDecimal(14)
        viewModel.onSalePriceEntered(newPrice)

        val expectedState = viewState.copy(
            pricingData = pricingData.copy(
                salePrice = newPrice
            ),
            salePriceErrorMessage = R.string.product_pricing_update_sale_price_error
        )

        assertThat(state).isEqualTo(expectedState)
    }

    @Test
    fun `Hides the tax section for variation pricing`() = testBlocking {
        val savedState = ProductPricingFragmentArgs(RequestCodes.VARIATION_DETAIL_PRICING, pricingData)
            .toSavedStateHandle()

        viewModel = spy(
            ProductPricingViewModel(
                savedState,
                productRepository,
                wooCommerceStore,
                selectedSite,
                parameterRepository,
                analyticsTracker
            )
        )

        var state: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> state = new }

        val expectedState = viewState.copy(
            taxClassList = null,
            isTaxSectionVisible = false
        )

        assertThat(state).isEqualTo(expectedState)
    }

    @Test
    fun `Makes sale end date equal to start date if earlier`() = testBlocking {
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
            pricingData = pricingData.copy(
                isSaleScheduled = true,
                saleStartDate = startDate,
                saleEndDate = startDate
            )
        )

        assertThat(state).isEqualTo(expectedState)
    }

    @Test
    fun `Nulls the sale schedule dates if switch off`() = testBlocking {
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
            pricingData = pricingData.copy(
                isSaleScheduled = true,
                saleStartDate = startDate,
                saleEndDate = endDate
            )
        )

        assertThat(state).isEqualTo(expectedState)

        givenScheduleDateIsDisabled()

        val resetState = viewState.copy(
            pricingData = pricingData.copy(
                isSaleScheduled = false,
                saleStartDate = startDate,
                saleEndDate = endDate
            )
        )

        assertThat(state).isEqualTo(resetState)

        viewModel.onExit()

        val expectedResultData = resetState.pricingData.copy(
            saleStartDate = null,
            saleEndDate = null
        )

        assertThat((firedEvent as? ExitWithResult<*>)?.data).isEqualTo(expectedResultData)
    }

    @Test
    fun `Displays sale price error message when sale price is greater than regular price`() =
        testBlocking {
            var state: ViewState? = null
            viewModel.viewStateData.observeForever { _, new -> state = new }

            givenProductPricesInput(regularPrice = 4, salePrice = 5)

            assertThat(state?.salePriceErrorMessage).isEqualTo(R.string.product_pricing_update_sale_price_error)
        }

    @Test
    fun `Hides sale price error message when sale price is less than regular price`() =
        testBlocking {
            var state: ViewState? = null
            viewModel.viewStateData.observeForever { _, new -> state = new }

            givenProductPricesInput(
                regularPrice = 2,
                salePrice = 1
            )

            assertThat(state?.salePriceErrorMessage).isEqualTo(0)
        }

    @Test
    fun `Display sale price error message when sale price is zero and regular price has negative value`() =
        testBlocking {
            var state: ViewState? = null
            viewModel.viewStateData.observeForever { _, new -> state = new }

            givenScheduleDateIsDisabled()
            givenProductPricesInput(
                regularPrice = -2,
                salePrice = 0
            )

            assertThat(state?.salePriceErrorMessage).isEqualTo(R.string.product_pricing_update_sale_price_error)
        }

    @Test
    fun `Hide sale price error message when sale price is null and regular price has any value`() =
        testBlocking {
            var state: ViewState? = null
            viewModel.viewStateData.observeForever { _, new -> state = new }

            givenScheduleDateIsDisabled()
            givenProductPricesInput(
                regularPrice = 2,
                salePrice = 0
            )

            assertThat(state?.salePriceErrorMessage).isEqualTo(0)
        }

    @Test
    fun `Send tracks event upon exit if there was no changes`() {
        // when
        viewModel.onExit()

        // then
        verify(analyticsTracker).track(
            AnalyticsEvent.PRODUCT_PRICE_SETTINGS_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to false)
        )
    }

    @Test
    fun `Send tracks event upon exit if there was a change`() {
        // when
        viewModel.onRegularPriceEntered(BigDecimal(31415))
        viewModel.onExit()

        // then
        verify(analyticsTracker).track(
            AnalyticsEvent.PRODUCT_PRICE_SETTINGS_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to true)
        )
    }

    private fun givenScheduleDateIsDisabled() {
        viewModel.onScheduledSaleChanged(false)
    }

    private fun givenProductPricesInput(regularPrice: Int, salePrice: Int) {
        viewModel.onRegularPriceEntered(BigDecimal(regularPrice))
        viewModel.onSalePriceEntered(BigDecimal(salePrice))
    }
}
