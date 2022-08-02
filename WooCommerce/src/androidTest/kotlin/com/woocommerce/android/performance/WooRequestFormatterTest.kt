package com.woocommerce.android.performance

import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooRequestFormatterTest {

    val sut = WooRequestFormatter

    @Test
    fun name() {
        val result = sut.formatRequestUrl(
            Request.Builder()
                .url(
                    "https://wp_api/rest/v1.1/jetpack-blogs/<blog_id>/rest-api/?path=/wc/v2/orders/276/shipment-trackings/&_method=get"
                )
                .build()
        )

        assertThat(result).isEqualTo(
            "https://wp_api/rest/v1.1/jetpack-blogs/<blog_id>/rest-api/?path=/wc/v2/orders/<order_id>/shipment-trackings/&_method=get"
        )
    }
}
