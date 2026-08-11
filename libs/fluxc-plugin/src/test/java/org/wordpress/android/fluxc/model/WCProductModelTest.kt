package org.wordpress.android.fluxc.model

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType

class WCProductModelTest {
    @Test
    fun `empty attributes json array return empty array`() {
        val attributes = JsonArray()

        val sut = WCProductModel().copy(
            attributes = attributes.toString()
        )

        val result = sut.getAttributeList()

        assertThat(result).isEmpty()
    }

    @Test
    fun `json attributes without option return empty option`() {
        val attribute = JsonObject().apply {
            addProperty("id",1)
            addProperty("name", "attribute name")
            addProperty("variation", false)
            addProperty("visible", true)
        }
        val attributes = JsonArray().apply {
            add(attribute)
        }

        val sut = WCProductModel().copy(
            attributes = attributes.toString()
        )

        val result = sut.getAttributeList()

        Assertions.assertThat(result).isNotEmpty
    }

    @Test
    fun `isConfigurable should return false when bundledItems is malformed JSON`() {
        val sut = WCProductModel().copy(
            type = CoreProductType.BUNDLE.value,
            bundledItems = "malformed: [#ˆ%*(!@*#ˆ%(*!#ˆ(%*!]"
        )

        val result = sut.isConfigurable

        assertThat(result).isFalse
    }

    @Test
    fun `given a bundle with a bundled item, when isConfigurable is checked, then it is configurable`() {
        val sut = WCProductModel().copy(
            type = CoreProductType.BUNDLE.value,
            bundledItems = """[{"bundled_item_id": 1, "product_id": 39, "quantity_min": 1, "quantity_max": 1}]"""
        )

        val result = sut.isConfigurable

        assertThat(result).isTrue
    }

    @Test
    fun `given a bundle without bundled items, when isConfigurable is checked, then it is not configurable`() {
        val sut = WCProductModel().copy(
            type = CoreProductType.BUNDLE.value,
            bundledItems = "[]"
        )

        val result = sut.isConfigurable

        assertThat(result).isFalse
    }

    @Test
    fun `given a product which is not a bundle, when isConfigurable is checked, then it is not configurable`() {
        val sut = WCProductModel().copy(
            type = CoreProductType.SIMPLE.value,
            bundledItems = """[{"bundled_item_id": 1, "product_id": 39, "quantity_min": 1, "quantity_max": 1}]"""
        )

        val result = sut.isConfigurable

        assertThat(result).isFalse
    }

    @Test
    fun `when comparing products with the same images, then the images are the same`() {
        val sut = WCProductModel().copy(images = imagesJson(alt = "alt text"))
        val updatedProduct = WCProductModel().copy(images = imagesJson(alt = "alt text"))

        val result = sut.hasSameImages(updatedProduct)

        assertThat(result).isTrue
    }

    @Test
    fun `when comparing products with a different image alt text, then the images differ`() {
        val sut = WCProductModel().copy(images = imagesJson(alt = "alt text"))
        val updatedProduct = WCProductModel().copy(images = imagesJson(alt = "updated alt text"))

        val result = sut.hasSameImages(updatedProduct)

        assertThat(result).isFalse
    }

    @Test
    fun `when comparing products with a different image name, then the images differ`() {
        val sut = WCProductModel().copy(images = imagesJson(name = "name"))
        val updatedProduct = WCProductModel().copy(images = imagesJson(name = "updated name"))

        val result = sut.hasSameImages(updatedProduct)

        assertThat(result).isFalse
    }

    private fun imagesJson(alt: String = "", name: String = "") = JsonArray().apply {
        add(
            JsonObject().apply {
                addProperty("id", 1L)
                addProperty("src", "https://example.com/image.jpg")
                addProperty("alt", alt)
                addProperty("name", name)
            }
        )
    }.toString()
}
