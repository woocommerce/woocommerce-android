package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.WCProductVariationModel

class ProductVariationMapperTest {
    @Test
    fun `given the variation image was removed, when building the update body, then image id 0 is sent`() {
        val stored = WCProductVariationModel(image = """{"id":123,"src":"https://example.com/img.jpg"}""")
        val updated = stored.copy(image = "")

        val body = ProductVariationMapper.variantModelToProductJsonBody(stored, updated)

        val image = body["image"] as JsonObject
        assertThat(image.get("id").asLong).isEqualTo(0L)
        assertThat(image.has("src")).isFalse()
    }
}
