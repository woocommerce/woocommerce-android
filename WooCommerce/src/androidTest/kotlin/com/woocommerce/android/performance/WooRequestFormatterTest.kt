package com.woocommerce.android.performance

import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooRequestFormatterTest {
    private val sut = WooRequestFormatter

    @Test
    fun shouldDecodeUrl() {
        val result = sut.formatRequestUrl(
            Request.Builder()
                .url(
                    "https://test.com/?path=%2Fwc%2Fv2%2Fshipment-trackings%2F%26_method%3Dget&json=true"
                )
                .build()
        )

        assertThat(result).isEqualTo(
            "https://test.com/?path=/wc/v2/shipment-trackings/&_method=get&json=true"
        )
    }

    @Test
    fun shouldKeepOnlyPathAndMethodQueryParametersForRestApiCalls() {
        val result = sut.formatRequestUrl(
            Request.Builder()
                .url(
                    "https://test.com/rest-api/?path=%2Fwc%2Fv3%2Fproducts%2F%26_method%3Dget&json=true" +
                        "&query=%7B%22per_page%22%3A%2225%22%2C%22orderby%22%3A%22title%22%2C%22order%22%3A%22asc" +
                        "%22%2C%22offset%22%3A%220%22%2C%22include%22%3A%2231%22%7D&locale=en_US"
                )
                .build()
        )

        assertThat(result).isEqualTo(
            "https://test.com/rest-api/?path=/wc/v3/products/&_method=get"
        )
    }

    @Test
    fun shouldReplaceBlogIdWithPlaceholder() {
        val result = sut.formatRequestUrl(
            Request.Builder()
                .url(
                    "https://public-api.wordpress.com/123456789/"
                )
                .build()
        )

        assertThat(result).isEqualTo(
            "https://public-api.wordpress.com/<blog_id>/"
        )
    }

    @Test
    fun shouldReplaceOrderIdWithPlaceholder() {
        val result = sut.formatRequestUrl(
            Request.Builder()
                .url(
                    "https://public-api.wordpress.com/orders/123456/&_method=get"
                )
                .build()
        )

        assertThat(result).isEqualTo(
            "https://public-api.wordpress.com/orders/<order_id>/&_method=get"
        )
    }

    @Test
    fun shouldReplaceLabelIdWithPlaceholder() {
        val result = sut.formatRequestUrl(
            Request.Builder()
                .url(
                    "https://public-api.wordpress.com/label/123456/&_method=get"
                )
                .build()
        )

        assertThat(result).isEqualTo(
            "https://public-api.wordpress.com/label/<label_id>/&_method=get"
        )
    }
}
