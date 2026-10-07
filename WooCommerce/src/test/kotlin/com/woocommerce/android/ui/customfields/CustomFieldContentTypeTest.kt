package com.woocommerce.android.ui.customfields

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CustomFieldContentTypeTest {
    @Test
    fun `given a REST API URL, when resolving the content type, then it is TEXT`() {
        val value = "https://example.com/wp-json/wc/v3/payments/readers/receipts/pi_123"

        val result = CustomFieldContentType.fromMetadataValue(value)

        assertThat(result).isEqualTo(CustomFieldContentType.TEXT)
    }

    @Test
    fun `given a REST API URL using the rest_route parameter, when resolving the content type, then it is TEXT`() {
        val value = "https://example.com/index.php?rest_route=/wc/v3/orders"

        val result = CustomFieldContentType.fromMetadataValue(value)

        assertThat(result).isEqualTo(CustomFieldContentType.TEXT)
    }

    @Test
    fun `given a URL of a store page, when resolving the content type, then it is URL`() {
        val value = "https://example.com/wp-admin/post.php?post=1&action=edit"

        val result = CustomFieldContentType.fromMetadataValue(value)

        assertThat(result).isEqualTo(CustomFieldContentType.URL)
    }
}
