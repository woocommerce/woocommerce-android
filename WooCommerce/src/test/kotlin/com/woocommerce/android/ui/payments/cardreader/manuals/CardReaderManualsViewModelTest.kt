package com.woocommerce.android.ui.payments.cardreader.manuals

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.Chipper2X
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.StripeM2
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.WisePade3
import com.woocommerce.android.ui.payments.cardreader.manuals.CardReaderManualsViewModel.ManualItem
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@Suppress("UNCHECKED_CAST")
@ExperimentalCoroutinesApi
class CardReaderManualsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderManualsViewModel
    private val manualList: List<ManualItem> = mock()
    private val cardReaderManualSupportedCountryMapper: CardReaderManualsSupportedReadersMapper = mock()
    private val supportedCountryUs: CardReaderConfigForSupportedCountry = CardReaderConfigForUSA
    private val supportedCountryCA: CardReaderConfigForSupportedCountry = CardReaderConfigForCanada
    private val savedStateHandleCA = CardReaderManualsFragmentArgs(
        supportedCountryCA
    ).toSavedStateHandle()
    private val savedStateHandleUS = CardReaderManualsFragmentArgs(
        supportedCountryUs
    ).toSavedStateHandle()

    @Test
    fun `when screen shown, a manual list is displayed`() {
        whenever(cardReaderManualSupportedCountryMapper.mapSupportedReadersToManualItems(any(), any())).thenReturn(
            manualList
        )

        initViewModel(savedStateHandleUS)

        assertThat(viewModel.manualState)
            .isEqualTo(manualList)
    }

    @Test
    fun `given US store, when user click on chipper reader, then correct webview is displayed`() {
        whenever(cardReaderManualSupportedCountryMapper.mapSupportedReadersToManualItems(any(), any())).thenAnswer {
            listOf(
                ManualItem(
                    icon = drawable.ic_chipper_reader,
                    label = string.card_reader_bbpos_manual_card_reader,
                    onManualClicked = (it.arguments[1] as Map<ReaderType, () -> Unit>)[Chipper2X] as () -> Unit
                ),
            )
        }

        initViewModel(savedStateHandleUS)
        viewModel.manualState.find {
            it.label == string.card_reader_bbpos_manual_card_reader
        }?.onManualClicked?.invoke()

        assertThat(viewModel.event.value).isEqualTo(
            CardReaderManualsViewModel.ManualEvents.NavigateToCardReaderManualLink(AppUrls.BBPOS_MANUAL_CARD_READER)
        )
    }

    @Test
    fun `given US store, when user clicks on M2 reader, then correct webview is displayed`() {
        whenever(cardReaderManualSupportedCountryMapper.mapSupportedReadersToManualItems(any(), any())).thenAnswer {
            listOf(
                ManualItem(
                    icon = drawable.ic_m2_reader,
                    label = string.card_reader_m2_manual_card_reader,
                    onManualClicked = (it.arguments[1] as Map<ReaderType, () -> Unit>)[StripeM2] as () -> Unit
                ),
            )
        }

        initViewModel(savedStateHandleUS)
        viewModel.manualState.find {
            it.label == string.card_reader_m2_manual_card_reader
        }?.onManualClicked?.invoke()

        assertThat(viewModel.event.value).isEqualTo(
            CardReaderManualsViewModel.ManualEvents.NavigateToCardReaderManualLink(AppUrls.M2_MANUAL_CARD_READER)
        )
    }

    @Test
    fun `given CA store, when user clicks wisepa3, then correct webview is displayed`() {
        whenever(cardReaderManualSupportedCountryMapper.mapSupportedReadersToManualItems(any(), any())).thenAnswer {
            listOf(
                ManualItem(
                    icon = drawable.ic_wisepad3_reader,
                    label = string.card_reader_wisepad_3_manual_card_reader,
                    onManualClicked = (it.arguments[1] as Map<ReaderType, () -> Unit>)[WisePade3] as () -> Unit
                )
            )
        }
        initViewModel(savedStateHandleCA)
        viewModel.manualState.find {
            it.label == string.card_reader_wisepad_3_manual_card_reader
        }?.onManualClicked?.invoke()

        assertThat(viewModel.event.value).isEqualTo(
            CardReaderManualsViewModel.ManualEvents.NavigateToCardReaderManualLink(AppUrls.WISEPAD_3_MANUAL_CARD_READER)
        )
    }

    private fun initViewModel(savedStateHandle: SavedStateHandle) {
        viewModel = CardReaderManualsViewModel(
            savedStateHandle,
            cardReaderManualSupportedCountryMapper,
        )
    }
}
