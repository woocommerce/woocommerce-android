package com.woocommerce.android.ui.payments.cardreader.manuals

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForCanada
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
    private val supportedCountryUs: CardReaderConfigForSupportedCountry = CardReaderConfigForUSA
    private val supportedCountryCA: CardReaderConfigForSupportedCountry = CardReaderConfigForCanada
    private val selectedSite: SelectedSite = mock() {
        on { get() }.thenReturn(mock())
    }
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider = mock()

    @Test
    fun `when screen shown, a manual list is displayed`() {
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

    @Test
    fun `given US store, when user click on chipper reader, then correct webview is displayed`() {
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US")).thenReturn(supportedCountryUs)
        whenever(cardReaderManualSupportedCountryMapper.mapSupportedReadersToManualItems(any(), any())).thenReturn(
            listOf(
                ManualItem(
                    icon = drawable.ic_chipper_reader,
                    label = string.card_reader_bbpos_manual_card_reader,
                    onManualClicked = { viewModel.onBbposManualClicked() }
                ),
                ManualItem(
                    icon = drawable.ic_m2_reader,
                    label = string.card_reader_m2_manual_card_reader,
                    onManualClicked = { viewModel.onM2ManualClicked() }
                )
            )
        )

        initViewModel()
        viewModel.manualState[0].onManualClicked.invoke()

        assertThat(viewModel.event.value).isEqualTo(
            CardReaderManualsViewModel.ManualEvents.NavigateToCardReaderManualLink(AppUrls.BBPOS_MANUAL_CARD_READER)
        )
    }

    @Test
    fun `given US store, when user clicks on M2 reader, then correct webview is displayed`() {
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US")).thenReturn(supportedCountryUs)
        whenever(cardReaderManualSupportedCountryMapper.mapSupportedReadersToManualItems(any(), any())).thenReturn(
            listOf(
                ManualItem(
                    icon = drawable.ic_chipper_reader,
                    label = string.card_reader_bbpos_manual_card_reader,
                    onManualClicked = { viewModel.onBbposManualClicked() }
                ),
                ManualItem(
                    icon = drawable.ic_m2_reader,
                    label = string.card_reader_m2_manual_card_reader,
                    onManualClicked = { viewModel.onM2ManualClicked() }
                )
            )
        )

        initViewModel()
        viewModel.manualState[1].onManualClicked.invoke()

        assertThat(viewModel.event.value).isEqualTo(
            CardReaderManualsViewModel.ManualEvents.NavigateToCardReaderManualLink(AppUrls.M2_MANUAL_CARD_READER)
        )
    }

    @Test
    fun `given CA store, when user clicks wisepa3, then correct webview is displayed`() {
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("CA")
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("CA")).thenReturn(supportedCountryCA)
        whenever(cardReaderManualSupportedCountryMapper.mapSupportedReadersToManualItems(any(), any())).thenReturn(
            listOf(
                ManualItem(
                    icon = drawable.ic_wisepad3_reader,
                    label = string.card_reader_wisepad_3_manual_card_reader,
                    onManualClicked = { viewModel.onWisePad3ManualCardReaderClicked() }
                )
            )
        )
        initViewModel()
        viewModel.manualState[0].onManualClicked.invoke()

        assertThat(viewModel.event.value).isEqualTo(
            CardReaderManualsViewModel.ManualEvents.NavigateToCardReaderManualLink(AppUrls.WISEPAD_3_MANUAL_CARD_READER)
        )
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
