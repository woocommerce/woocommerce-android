package com.woocommerce.android.ui.orders.creation.product.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreateEditProductDetailsViewModelTest : BaseUnitTest() {
    private val tracker: AnalyticsTrackerWrapper = mock()

    @Test
    fun `when add discount tapped, then should track event`() = testBlocking {
        val savedState = OrderCreateEditProductDetailsFragmentArgs(
            Order.Item.EMPTY,
            "usd",
            true
        ).initSavedStateHandle()
        val sut = createSut(savedState)
        sut.onAddDiscountClicked()
        verify(tracker).track(AnalyticsEvent.ORDER_PRODUCT_DISCOUNT_ADD_BUTTON_TAPPED)
    }

    @Test
    fun `when edit discount tapped, then should track event`() = testBlocking {
        val savedState = OrderCreateEditProductDetailsFragmentArgs(
            Order.Item.EMPTY,
            "usd",
            true
        ).initSavedStateHandle()
        val sut = createSut(savedState)
        sut.onAddDiscountClicked()
        verify(tracker).track(AnalyticsEvent.ORDER_PRODUCT_DISCOUNT_ADD_BUTTON_TAPPED)
    }

    private fun createSut(savedState: SavedStateHandle) = OrderCreateEditProductDetailsViewModel(
        savedState,
        mock(),
        mock(),
        mock(),
        mock(),
        mock(),
        tracker
    )
}
