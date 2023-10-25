package com.woocommerce.android.ui.payments.customamounts

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CustomAmountsDialogViewModeltest : BaseUnitTest() {

    private val viewModel = CustomAmountsDialogViewModel(
        SavedStateHandle(),
    )

    @Test
    fun `when view model is initialised, then done button is not enabled`() {
        assertFalse(viewModel.viewState.isDoneButtonEnabled)
    }

    @Test
    fun `when custom amount is zero, then done button is not enabled`() {
        viewModel.currentPrice = BigDecimal.ZERO
        assertFalse(viewModel.viewState.isDoneButtonEnabled)
    }

    @Test
    fun `when custom amount is not zero, then done button is enabled`() {
        viewModel.currentPrice = BigDecimal.TEN
        assertTrue(viewModel.viewState.isDoneButtonEnabled)
    }
}
