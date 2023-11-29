package com.woocommerce.android.ui.payments.customamounts

import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_ADD_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_EDIT_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.PERCENTAGE_BASE
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel.CustomAmountType.PERCENTAGE_CUSTOM_AMOUNT
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.math.BigDecimal
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CustomAmountsDialogViewModelTest : BaseUnitTest() {

    private val tracker: AnalyticsTrackerWrapper = mock()
    private var viewModel = CustomAmountsDialogViewModel(
        CustomAmountsDialogArgs(
            customAmountUIModel = null,
            customAmountType = FIXED_CUSTOM_AMOUNT,
            orderTotal = null,
        ).toSavedStateHandle(),
        tracker
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

    @Test
    fun `when custom amount dialog is opened for adding, then proper event is tracked`() {
        verify(tracker).track(ORDER_CREATION_ADD_CUSTOM_AMOUNT_TAPPED)
    }

    @Test
    fun `when custom amount dialog is opened for editing, then proper event is tracked`() {
        viewModel = CustomAmountsDialogViewModel(
            CustomAmountsDialogArgs(
                customAmountUIModel = CustomAmountUIModel(
                    id = 0L,
                    amount = BigDecimal.TEN,
                    name = ""
                ),
                customAmountType = FIXED_CUSTOM_AMOUNT,
                orderTotal = null
            ).toSavedStateHandle(),
            tracker
        )

        verify(tracker).track(ORDER_CREATION_EDIT_CUSTOM_AMOUNT_TAPPED)
    }

    // region Percentage based custom amounts
    @Test
    fun `when percentage is zero, then done button is not enabled`() {
        viewModel.currentPercentage = BigDecimal.ZERO

        assertFalse(viewModel.viewState.isDoneButtonEnabled)
    }

    @Test
    fun `when percentage is not zero, then done button is enabled`() {
        viewModel.currentPercentage = BigDecimal.TEN

        assertTrue(viewModel.viewState.isDoneButtonEnabled)
    }

    @Test
    fun `when custom amount is opened for editing in percentage mode, then proper percentage is calculated from current price`() {
        viewModel = CustomAmountsDialogViewModel(
            CustomAmountsDialogArgs(
                customAmountUIModel = CustomAmountUIModel(
                    id = 0L,
                    amount = BigDecimal.TEN,
                    name = ""
                ),
                customAmountType = PERCENTAGE_CUSTOM_AMOUNT,
                orderTotal = "200"
            ).toSavedStateHandle(),
            tracker
        )

        assertThat(viewModel.currentPercentage).isEqualTo(BigDecimal("5.00"))
    }

    //endregion
}
