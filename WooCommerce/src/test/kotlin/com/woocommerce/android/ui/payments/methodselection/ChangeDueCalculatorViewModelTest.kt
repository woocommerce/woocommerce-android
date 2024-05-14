package com.woocommerce.android.ui.payments.methodselection

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.methodselection.ChangeDueCalculatorViewModel.UiState
import com.woocommerce.android.viewmodel.BaseUnitTest
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.DefaultAsserter.assertEquals

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
        // TODO
    }
}

