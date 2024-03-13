package com.woocommerce.android.ui.prefs

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.model.UiString
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperOptionsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: DeveloperOptionsViewModel

    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val developerOptionsRepository: DeveloperOptionsRepository = mock()
    private val cardReaderManager: CardReaderManager = mock()

    @Before
    fun setup() {
        initViewModel()
    }

    @Test
    fun `when dev options screen accessed, then enable simulated reader label is displayed`() {
        val simulatedReaderRow = viewModel.viewState.value?.rows?.find {
            it.label == UiString.UiStringRes(R.string.enable_card_reader)
        }

        assertThat(simulatedReaderRow).isNotNull
    }

    @Test
    fun `when dev options screen accessed, then enable simulated reader icon is displayed`() {
        val simulatedReaderRow = viewModel.viewState.value?.rows?.find {
            it.icon == R.drawable.img_card_reader_connecting
        }

        assertThat(simulatedReaderRow).isNotNull
    }

    @Test
    fun `when simulated card reader btn toggled, then simulated reader state is enabled`() {
        testBlocking {
            whenever(developerOptionsRepository.isSimulatedCardReaderEnabled()).thenReturn(true)

            initViewModel()

            assertThat(
                (
                    viewModel.viewState.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.enable_card_reader)
                    } as DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.ToggleableListItem
                    ).isChecked
            ).isTrue()
        }
    }

    @Test
    fun `when simulated card reader btn untoggled, then simulated reader state is disabled`() {
        testBlocking {
            whenever(developerOptionsRepository.isSimulatedCardReaderEnabled()).thenReturn(false)

            initViewModel()

            assertThat(
                (
                    viewModel.viewState.value?.rows?.find {
                        it.label == UiString.UiStringRes(R.string.enable_card_reader)
                    } as DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.ToggleableListItem
                    ).isChecked
            ).isFalse()
        }
    }

    @Test
    fun `given reader enabled, when dev options screen accessed, then update simulated reader row displayed`() {
        whenever(developerOptionsRepository.isSimulatedCardReaderEnabled()).thenReturn(true)

        initViewModel()

        assertThat(viewModel.viewState.value?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.update_simulated_reader)
            }
    }

    @Test
    fun `when simulated card reader btn toggled, then interac row displayed`() {
        whenever(developerOptionsRepository.isSimulatedCardReaderEnabled()).thenReturn(true)

        initViewModel()

        assertThat(viewModel.viewState.value?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.enable_interac_payment)
            }
    }

    @Test
    fun `given reader disabled, when dev options screen accessed, then update reader row not displayed`() {
        whenever(developerOptionsRepository.isSimulatedCardReaderEnabled()).thenReturn(false)

        initViewModel()

        assertThat(viewModel.viewState.value?.rows)
            .noneMatch {
                it.label == UiString.UiStringRes(R.string.update_simulated_reader)
            }
    }

    @Test
    fun `given reader disabled, when dev options screen accessed, then interac row not displayed`() {
        whenever(developerOptionsRepository.isSimulatedCardReaderEnabled()).thenReturn(false)

        initViewModel()

        assertThat(viewModel.viewState.value?.rows)
            .noneMatch {
                it.label == UiString.UiStringRes(R.string.enable_interac_payment)
            }
    }

    private fun initViewModel() {
        viewModel = DeveloperOptionsViewModel(
            savedStateHandle,
            developerOptionsRepository,
            cardReaderManager,
            mock()
        )
    }
}
