package com.woocommerce.android.ui.orders.creation.product.details

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import com.woocommerce.android.ui.orders.creation.ProductInfo
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class OrderCreateEditProductDetailsViewModelTest : BaseUnitTest() {
    private val tracker: AnalyticsTrackerWrapper = mock()

    private val defaultProduct = OrderCreationProduct.ProductItem(
        item = Order.Item.EMPTY,
        productInfo = ProductInfo(
            imageUrl = "",
            isStockManaged = false,
            stockQuantity = 0.0,
            stockStatus = ProductStockStatus.InStock
        )
    )

    @Test
    fun `when add discount tapped, then should track event`() = testBlocking {
        val savedState = OrderCreateEditProductDetailsFragmentArgs(
            defaultProduct,
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
            defaultProduct,
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
        tracker
    )
}
