package com.woocommerce.android.ui.orders.creation.taxes

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRate
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateRepository
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateSelectorViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify


@OptIn(ExperimentalCoroutinesApi::class)
internal class TaxRateSelectorViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: TaxRateSelectorViewModel
    private val tracker: AnalyticsTrackerWrapper = mock()
    private val savedStateHandle: SavedStateHandle = mock()
    private val repository: TaxRateRepository = mock()
    lateinit var selectedSite: SelectedSite
    protected lateinit var resourceProvider: ResourceProvider


    @Before
    fun setup() {
        // Initialize the ViewModel with mocked dependencies
        viewModel = TaxRateSelectorViewModel(tracker, repository, savedStateHandle)
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
}
