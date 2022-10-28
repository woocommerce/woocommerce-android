package com.woocommerce.android.ui.prefs

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperOptionsTest : BaseUnitTest() {
    private lateinit var viewModel: DeveloperOptionsViewModel

    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val developerOptionsRepository: DeveloperOptionsRepository = mock()

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

    private fun initViewModel() {
        viewModel = DeveloperOptionsViewModel(
            savedStateHandle,
            developerOptionsRepository
        )
    }
}
