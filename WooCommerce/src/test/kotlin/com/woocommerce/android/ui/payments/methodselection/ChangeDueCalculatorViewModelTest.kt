package com.woocommerce.android.ui.payments.methodselection

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

@ExperimentalCoroutinesApi
class ChangeDueCalculatorViewModelTest : BaseUnitTest() {
    @Test
    fun `given valid order id, when order details are requested, then success state is emitted`() = testBlocking {
        // GIVEN
        // TODO val viewModel: ChangeDueCalculatorViewModel = mock()
        // TODO

        // WHEN
        // TODO

        // THEN
        // TODO
    }

    @Test
    fun `given order details retrieval failure, when order details are loaded, then error state is emitted`() = testBlocking {
        // GIVEN
        // TODO val viewModel: ChangeDueCalculatorViewModel = mock()

        // WHEN
        // TODO

        // THEN
        // TODO assertThat(viewModel.uiState.value).isEqualTo(ChangeDueCalculatorViewModel.UiState.Error)
    }
}
