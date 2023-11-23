package com.woocommerce.android.ui.subscriptions

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.SubscriptionPeriod
import com.woocommerce.android.ui.products.subscriptions.ProductSubscriptionFreeTrialFragmentArgs
import com.woocommerce.android.ui.products.subscriptions.ProductSubscriptionFreeTrialViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ProductSubscriptionFreeTrialViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ProductSubscriptionFreeTrialViewModel
    private val savedStateHandle: SavedStateHandle = ProductSubscriptionFreeTrialFragmentArgs(
        com.woocommerce.android.ui.products.ProductHelper.getDefaultSubscriptionDetails().copy(
            length = 10
        )
    ).initSavedStateHandle()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    @Before
    fun setup() {
        viewModel = ProductSubscriptionFreeTrialViewModel(
            analyticsTrackerWrapper,
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
