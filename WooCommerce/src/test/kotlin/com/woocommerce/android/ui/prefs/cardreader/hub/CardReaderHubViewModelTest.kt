package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class CardReaderHubViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderHubViewModel

    @Before
    fun setUp() {
        viewModel = CardReaderHubViewModel(SavedStateHandle())
    }

    @Test
    fun `when screen shown, then manage card reader row present`() {
        assertThat((viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
            }
    }

    @Test
    fun `when screen shown, then purchase card reader row present`() {
        assertThat((viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
            }
    }

    @Test
    fun `when screen shown, then manual card reader row present`() {
        assertThat((viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_manual_card_reader)
            }
    }

    @Test
    fun `when screen shown, then manual card reader row present at the last`() {
        assertThat((viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows)
            .last().matches {
                it.label == UiString.UiStringRes(R.string.card_reader_manual_card_reader)
            }
    }

    @Test
    fun `when user clicks on manage card reader, then app navigates to card reader detail screen`() {
        (viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows
            .find {
                it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
            }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderDetail)
    }

    @Test
    fun `when user clicks on purchase card reader, then app opens external webview`() {
        (viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows
            .find {
                it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
            }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow)
    }

    @Test
    fun `when user clicks on manual card reader, then app opens external webview`() {
        (viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows
            .find {
                it.label == UiString.UiStringRes(R.string.card_reader_manual_card_reader)
            }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(CardReaderHubViewModel.CardReaderHubEvents.NavigateToManualCardReaderFlow)
    }
}
