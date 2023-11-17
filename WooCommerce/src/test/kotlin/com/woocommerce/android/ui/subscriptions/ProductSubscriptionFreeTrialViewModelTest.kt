package com.woocommerce.android.ui.subscriptions

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.SubscriptionDetails
import com.woocommerce.android.model.SubscriptionPeriod
import com.woocommerce.android.model.SubscriptionPeriod.Month
import com.woocommerce.android.ui.products.subscriptions.ProductSubscriptionFreeTrialFragmentArgs
import com.woocommerce.android.ui.products.subscriptions.ProductSubscriptionFreeTrialViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class ProductSubscriptionFreeTrialViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ProductSubscriptionFreeTrialViewModel
    private val savedStateHandle: SavedStateHandle = ProductSubscriptionFreeTrialFragmentArgs(
        SubscriptionDetails(
            price = BigDecimal(10),
            period = Month,
            periodInterval = 1,
            length = 10,
            signUpFee = null,
            trialPeriod = null,
            trialLength = null,
            oneTimeShipping = false
        )
    ).initSavedStateHandle()

    @Before
    fun setup() {
        viewModel = ProductSubscriptionFreeTrialViewModel(
            savedStateHandle
        ).apply {
            viewState.observeForever { _ -> }
        }
    }

    @Test
    fun `verify state update when length changed`() = testBlocking {
        val newLength = 5

        viewModel.onLengthChanged(newLength)
        advanceUntilIdle()

        assertThat(viewModel.viewState.value?.length).isEqualTo(newLength)
    }

    @Test
    fun `verify state update when period changed`() = testBlocking {
        val newPeriod = SubscriptionPeriod.Week

        viewModel.onPeriodChanged(newPeriod)
        advanceUntilIdle()

        assertThat(viewModel.viewState.value?.period).isEqualTo(newPeriod)
    }
}
