package com.woocommerce.android.ui.payments.methodselection

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class ChangeDueCalculatorViewModelTest : BaseUnitTest() {

    private val viewModel: ChangeDueCalculatorViewModel = mock()

    @Test
    fun `given valid order id, when order details are requested, then success state is emitted`() = testBlocking {
        // GIVEN
        // TODO

        // WHEN
        viewModel.loadOrderDetails()

        // THEN
        // TODO
    }

    @Test
    fun `given order details retrieval failure, when order details are loaded, then error state is emitted`() = testBlocking {
        // GIVEN
        // TODO

        // WHEN
        viewModel.loadOrderDetails()

        // THEN
        // TODO assertThat(viewModel.uiState.value).isEqualTo(ChangeDueCalculatorViewModel.UiState.Error)
    }
}
