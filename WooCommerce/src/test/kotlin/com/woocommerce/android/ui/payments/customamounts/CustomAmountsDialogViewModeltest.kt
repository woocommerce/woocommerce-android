package com.woocommerce.android.ui.payments.customamounts

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_ADD_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_EDIT_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.math.BigDecimal
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CustomAmountsDialogViewModeltest : BaseUnitTest() {

    private val tracker: AnalyticsTrackerWrapper = mock()

    @Test
    fun `when view model is initialised, then done button is not enabled`() {
        val viewModel = CustomAmountsDialogViewModel(
            SavedStateHandle(),
            tracker
        )

        assertFalse(viewModel.viewState.isDoneButtonEnabled)
    }

    @Test
    fun `when custom amount is zero, then done button is not enabled`() {
        val viewModel = CustomAmountsDialogViewModel(
            SavedStateHandle(),
            tracker
        )

        viewModel.currentPrice = BigDecimal.ZERO

        assertFalse(viewModel.viewState.isDoneButtonEnabled)
    }

    @Test
    fun `when custom amount is not zero, then done button is enabled`() {
        val viewModel = CustomAmountsDialogViewModel(
            SavedStateHandle(),
            tracker
        )

        viewModel.currentPrice = BigDecimal.TEN

        assertTrue(viewModel.viewState.isDoneButtonEnabled)
    }

    @Test
    fun `when custom amount dialog is opened for adding, then proper event is tracked`() {
        CustomAmountsDialogViewModel(
            CustomAmountsDialogArgs(customAmountUIModel = null).toSavedStateHandle(),
            tracker
        )

        verify(tracker).track(ORDER_CREATION_ADD_CUSTOM_AMOUNT_TAPPED)
    }

    @Test
    fun `when custom amount dialog is opened for editing, then proper event is tracked`() {
        CustomAmountsDialogViewModel(
            CustomAmountsDialogArgs(
                customAmountUIModel = CustomAmountUIModel(
                    id = 0L,
                    amount = BigDecimal.TEN,
                    name = ""
                )
            ).toSavedStateHandle(),
            tracker
        )

        verify(tracker).track(ORDER_CREATION_EDIT_CUSTOM_AMOUNT_TAPPED)
    }
}
