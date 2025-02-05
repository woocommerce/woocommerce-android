package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.GsonBuilder
import org.junit.Test
import org.wordpress.android.fluxc.UnitTestUtils
import kotlin.test.assertEquals

class BatchOrderApiResponseTest {
    @Test
    fun testDeserializeBatchOrderResponse() {
        val testGson = GsonBuilder()
            .create()

        val batchOrderJson = UnitTestUtils.getStringFromResourceFile(
            this.javaClass, "wc/orders-batch.json"
        )

        val response = testGson.fromJson(batchOrderJson, BatchOrderApiResponse::class.java)
        val orders = response.update

        assertEquals(2, orders.size)

        val firstOrder = orders[0] as BatchOrderApiResponse.OrderResponse.Success
        assertEquals(1032L, firstOrder.order.id)
        assertEquals("224.00", firstOrder.order.total)

        val secondOrder = orders[1] as BatchOrderApiResponse.OrderResponse.Error
        assertEquals(525L, secondOrder.id)
        assertEquals("woocommerce_rest_shop_order_invalid_id", secondOrder.error.code)
        assertEquals(400, secondOrder.error.data.status)
    }
}
