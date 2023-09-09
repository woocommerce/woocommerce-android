package com.woocommerce.android.ui.orders.creation.taxes

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRate
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateListHandler
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateRepository
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateSelectorFragmentArgs
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateSelectorViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class TaxRateSelectorViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: TaxRateSelectorViewModel
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val savedStateHandle: SavedStateHandle = TaxRateSelectorFragmentArgs(mock()).initSavedStateHandle()
    private val repository: TaxRateRepository = mock()
    private val taxRateListHandler: TaxRateListHandler = mock()
    private val result: Result<Boolean> = mock()

    @Before
    fun setup() {
        testBlocking { whenever(repository.fetchTaxRates(1, 10)).thenReturn(result) }
        viewModel = TaxRateSelectorViewModel(tracker, taxRateListHandler, savedStateHandle)
    }

    @Test
    fun `when tax rate selected, then should track event`() = testBlocking {
        // Create a TaxRateUiModel for testing
        val taxRate = TaxRate(1, "US", "NY", "12345", "New York")
        val taxRateUiModel = TaxRateSelectorViewModel.TaxRateUiModel(
            "Test Rate · US NY 12345 New York", "10%", taxRate
        )

        // WHEN
        viewModel.onTaxRateSelected(taxRateUiModel)

        // THEN
        verify(tracker).track(AnalyticsEvent.TAX_RATE_SELECTOR_TAX_RATE_TAPPED)
    }

    @Test
    fun `when onEditTaxRatesInAdmin clicked, the should track event`() = testBlocking {

        // WHEN
        viewModel.onEditTaxRatesInAdminClicked()

        // THEN
        verify(tracker).track(AnalyticsEvent.TAX_RATE_SELECTOR_EDIT_IN_ADMIN_TAPPED)
    }
}
