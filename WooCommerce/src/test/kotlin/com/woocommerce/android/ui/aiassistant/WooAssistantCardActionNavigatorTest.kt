package com.woocommerce.android.ui.aiassistant

import com.woocommerce.android.R
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooAssistantCardActionNavigatorTest {
    @Test
    fun `given open order action, when mapped, then order details direction is returned`() {
        val direction = AssistantCardAction.OpenOrder(remoteOrderId = 123L).toNavDirections()

        assertThat(direction.actionId).isEqualTo(R.id.action_global_orderDetailFragment)
        assertThat(direction).isEqualTo(
            NavGraphMainDirections.actionGlobalOrderDetailFragment(
                orderId = 123L,
                ignoreTwoPaneLayoutLogic = true,
            )
        )
    }

    @Test
    fun `given open product action, when mapped, then product details direction is returned`() {
        val direction = AssistantCardAction.OpenProduct(remoteProductId = 456L).toNavDirections()

        assertThat(direction.actionId).isEqualTo(R.id.action_global_productDetailFragment)
        assertThat(direction).isEqualTo(
            NavGraphMainDirections.actionGlobalProductDetailFragment(
                mode = ProductDetailFragment.Mode.ShowProduct(remoteProductId = 456L),
            )
        )
    }
}
