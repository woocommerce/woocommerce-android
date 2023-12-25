package com.woocommerce.android.ui.payments.customamounts

import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_ADD_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_EDIT_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsViewModel.CustomAmountType.PERCENTAGE_CUSTOM_AMOUNT
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.math.BigDecimal
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CustomAmountsFragmentViewModelTest : BaseUnitTest() {

    private val tracker: AnalyticsTrackerWrapper = mock()
    private lateinit var viewModel: CustomAmountsViewModel

    @Before
    fun setup() {
        viewModel = CustomAmountsViewModel(
            CustomAmountsFragmentArgs(
                customAmountUIModel = CustomAmountUIModel.EMPTY,
                orderTotal = null,
            ).toSavedStateHandle(),
            tracker
        )
    }

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
        viewModel = CustomAmountsViewModel(
            CustomAmountsFragmentArgs(
                customAmountUIModel = CustomAmountUIModel(
                    id = 0L,
                    amount = BigDecimal.TEN,
                    name = "",
                    type = FIXED_CUSTOM_AMOUNT
                ),
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
        viewModel = CustomAmountsViewModel(
            CustomAmountsFragmentArgs(
                customAmountUIModel = CustomAmountUIModel(
                    id = 0L,
                    amount = BigDecimal.TEN,
                    name = "",
                    type = FIXED_CUSTOM_AMOUNT
                ),
                orderTotal = "200"
            ).toSavedStateHandle(),
            tracker
        )

        assertThat(viewModel.currentPercentage).isEqualTo(BigDecimal("5.00"))
    }

    @Test
    fun `when custom amount is modified by adding a certain percentage, then proper current price is calculated`() {
        viewModel = CustomAmountsViewModel(
            CustomAmountsFragmentArgs(
                customAmountUIModel = CustomAmountUIModel(
                    id = 0L,
                    amount = BigDecimal.TEN,
                    name = "",
                    type = FIXED_CUSTOM_AMOUNT
                ),
                orderTotal = "200"
            ).toSavedStateHandle(),
            tracker
        )
        viewModel.currentPercentage = BigDecimal("20")

        assertThat(viewModel.viewState.customAmountUIModel.currentPrice).isEqualTo(BigDecimal("40.00"))
    }
    @Test
    fun `when view model is initiated as fixed custom amount type, then assign proper custom amount type to view state`() {
        viewModel = CustomAmountsViewModel(
            CustomAmountsFragmentArgs(
                customAmountUIModel = CustomAmountUIModel(
                    id = 0L,
                    amount = BigDecimal.TEN,
                    name = "",
                    type = FIXED_CUSTOM_AMOUNT
                ),
                orderTotal = "200"
            ).toSavedStateHandle(),
            tracker
        )
        assertThat(viewModel.viewState.customAmountUIModel.type).isEqualTo(FIXED_CUSTOM_AMOUNT)
    }

    @Test
    fun `when view model is initiated as percentage custom amount type, then assign proper custom amount type to view state`() {
        viewModel = CustomAmountsViewModel(
            CustomAmountsFragmentArgs(
                customAmountUIModel = CustomAmountUIModel(
                    id = 0L,
                    amount = BigDecimal.TEN,
                    name = "",
                    type = PERCENTAGE_CUSTOM_AMOUNT
                ),
                orderTotal = "200"
            ).toSavedStateHandle(),
            tracker
        )
        assertThat(viewModel.viewState.customAmountUIModel.type).isEqualTo(PERCENTAGE_CUSTOM_AMOUNT)
    }

    @Test
    fun `when custom amount dialog is opened to edit, then update view state to populate all values`() {
        val customAmountUIModel = CustomAmountsFragmentArgs(
            customAmountUIModel = CustomAmountUIModel(
                id = 0L,
                amount = BigDecimal.TEN,
                name = "",
                type = PERCENTAGE_CUSTOM_AMOUNT
            ),
            orderTotal = "200"
        )
        viewModel = CustomAmountsViewModel(
            customAmountUIModel.toSavedStateHandle(),
            tracker
        )
        assertThat(viewModel.viewState.customAmountUIModel.id).isEqualTo(
            customAmountUIModel.customAmountUIModel.id
        )
        assertThat(viewModel.viewState.customAmountUIModel.currentPrice).isEqualTo(
            customAmountUIModel.customAmountUIModel.amount
        )
        assertThat(viewModel.viewState.customAmountUIModel.name).isEqualTo(
            customAmountUIModel.customAmountUIModel.name
        )
        assertThat(viewModel.viewState.customAmountUIModel.type).isEqualTo(
            customAmountUIModel.customAmountUIModel.type
        )
        assertThat(viewModel.viewState.customAmountUIModel.taxStatus).isEqualTo(
            customAmountUIModel.customAmountUIModel.taxStatus
        )
    }

    //endregion
}
