package com.woocommerce.android.ui.prefs.cardreader.connect

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.NavigationTarget
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CardReaderConnectViewModelTest : BaseUnitTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

    private lateinit var viewModel: CardReaderConnectViewModel

    @Before
    fun setUp() {
        viewModel = CardReaderConnectViewModel(
            SavedStateWithArgs(SavedStateHandle(), null, null),
            coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `when initiate scan button clicked, then the app navigates to scan screen`() {
        viewModel.onInitiateScanBtnClicked()

        assertThat(viewModel.event.value)
            .isInstanceOf(NavigationTarget.CardReaderScanScreen::class.java)
    }
}
