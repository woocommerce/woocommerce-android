package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.WCProductVariationModel

class ProductVariationMapperTest {
    @Test
    fun `given the variation image was removed, when building the update body, then image id 0 is sent`() {
        val stored = WCProductVariationModel(image = """{"id":123,"src":"https://example.com/img.jpg"}""")
        val updated = stored.copy(editContextImage = "")

        val body = ProductVariationMapper.variantModelToProductJsonBody(stored, updated)

        val image = body["image"] as JsonObject
        assertThat(image.get("id").asLong).isEqualTo(0L)
        assertThat(image.has("src")).isFalse()
    }

    @Test
    fun `given the variation has no own image, when building an update without image changes, then image is not sent`() {
        val stored = WCProductVariationModel(
            image = """{"id":123,"src":"https://example.com/parent.jpg"}""",
            editContextImage = ""
        )
        val updated = stored.copy(sku = "new-sku")

        val body = ProductVariationMapper.variantModelToProductJsonBody(stored, updated)

        assertThat(body).doesNotContainKey("image")
    }

    @Test
    fun `given the same image id in different json formats, when building the update body, then image is not sent`() {
        val stored = WCProductVariationModel(
            editContextImage = """{"id":123,"name":"img","src":"https://example.com/img.jpg","alt":""}"""
        )
        val updated = stored.copy(
            editContextImage = """{"id":123,"name":"img","src":"https://example.com/img.jpg","date_created_gmt":""}"""
        )

        val body = ProductVariationMapper.variantModelToProductJsonBody(stored, updated)

        assertThat(body).doesNotContainKey("image")
    }

    @Test
    fun `given the updated model carries no edit context image, when building the update body, then image is not sent`() {
        val stored = WCProductVariationModel(image = """{"id":123,"src":"https://example.com/img.jpg"}""")
        val updated = stored.copy(sku = "new-sku")

        val body = ProductVariationMapper.variantModelToProductJsonBody(stored, updated)

        assertThat(body).doesNotContainKey("image")
    }
}
