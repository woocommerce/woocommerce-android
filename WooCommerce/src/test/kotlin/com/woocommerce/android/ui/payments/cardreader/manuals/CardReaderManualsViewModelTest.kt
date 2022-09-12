package com.woocommerce.android.ui.payments.cardreader.manuals

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUSA
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.manuals.CardReaderManualsViewModel.ManualItem
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class CardReaderManualsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderManualsViewModel
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val wooStore: WooCommerceStore = mock()
    private val manualList: List<ManualItem> = mock()

    private val cardReaderManualSupportedCountryMapper: CardReaderManualsSupportedReadersMapper = mock()
    private val selectedSite: SelectedSite = mock() {
        on { get() }.thenReturn(mock())
    }
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider = mock()

    @Test
    fun `when screen shown, then BBPOS label is displayed`() {
        val supportedCountry: CardReaderConfigForSupportedCountry = CardReaderConfigForUSA
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US")).thenReturn(supportedCountry)
        whenever(cardReaderManualSupportedCountryMapper.mapSupportedReadersToManualItems(any(), any())).thenReturn(
            manualList
        )

        initViewModel()

        assertThat(viewModel.manualState)
            .isEqualTo(manualList)
    }

    private fun initViewModel() {
        viewModel = CardReaderManualsViewModel(
            savedStateHandle,
            selectedSite,
            wooStore,
            cardReaderManualSupportedCountryMapper,
            cardReaderCountryConfigProvider,
        )
    }
}
