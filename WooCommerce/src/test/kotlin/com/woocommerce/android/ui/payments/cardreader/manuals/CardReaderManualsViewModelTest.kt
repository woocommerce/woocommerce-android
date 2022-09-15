package com.woocommerce.android.ui.payments.cardreader.manuals

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CardReaderManualsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderManualsViewModel
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()

    @Before
    fun setUp() {
        viewModel = CardReaderManualsViewModel(savedStateHandle)
    }

    @Test
    fun `when screen shown, then BBPOS label is displayed`() {
        val bbposRow = viewModel.manualState.find {
            it.label == R.string.card_reader_bbpos_manual_card_reader
        }

        assertThat(bbposRow).isNotNull
    }

    @Test
    fun `when user clicks BBPOS reader, then app navigates to BBPOS manual link`() {
        val bbposRow = viewModel.manualState.find {
            it.label == R.string.card_reader_bbpos_manual_card_reader
        }

        bbposRow!!.onManualClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderManualsViewModel.ManualEvents.NavigateToCardReaderManualLink(
                    AppUrls.BBPOS_MANUAL_CARD_READER
                )
            )
    }

    @Test
    fun `when screen shown, then BBPOS icon is displayed`() {
        val bbposRow = viewModel.manualState.find {
            it.icon == R.drawable.ic_chipper_reader
        }

        assertThat(bbposRow).isNotNull
    }

    @Test
    fun `when screen shown, then M2 label is displayed`() {
        val m2Row = viewModel.manualState.find {
            it.label == R.string.card_reader_m2_manual_card_reader
        }

        assertThat(m2Row).isNotNull
    }

    @Test
    fun `when user clicks M2 reader, then app navigates to M2 manual link`() {
        val m2Row = viewModel.manualState.find {
            it.label == R.string.card_reader_m2_manual_card_reader
        }

        m2Row!!.onManualClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderManualsViewModel.ManualEvents.NavigateToCardReaderManualLink(
                    AppUrls.M2_MANUAL_CARD_READER
                )
            )
    }

    @Test
    fun `when screen is shown, then M2 icon is displayed`() {
        val m2Row = viewModel.manualState.find {
            it.icon == R.drawable.ic_m2_reader
        }

        assertThat(m2Row).isNotNull
    }

    @Test
    fun `when screen shown, then wisepad3 label is displayed`() {

        val wisePad3 = viewModel.manualState.find {
            it.label == R.string.card_reader_wisepad_3_manual_card_reader
        }

        assertThat(wisePad3).isNotNull
    }

    @Test
    fun `when user clicks wisepad3 reader, then app navigates to wisepad3 manual link`() {

        val wisePad3 = viewModel.manualState.find {
            it.label == R.string.card_reader_wisepad_3_manual_card_reader
        }

        wisePad3!!.onManualClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderManualsViewModel.ManualEvents.NavigateToCardReaderManualLink(
                    AppUrls.WISEPAD_3_MANUAL_CARD_READER
                )
            )
    }

    @Test
    fun `when screen is shown, then wisepad3 icon is displayed`() {

        val wisePad3 = viewModel.manualState.find {
            it.icon == R.drawable.ic_wisepad3_reader
        }

        assertThat(wisePad3).isNotNull
    }
}
