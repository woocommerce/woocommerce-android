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
    fun `given attribute json with flags and position, when parsed, then they are kept`() {
        // GIVEN
        val attribute = JsonObject().apply {
            addProperty("id", 0)
            addProperty("name", "Material")
            addProperty("variation", false)
            addProperty("visible", false)
            addProperty("position", 2)
            add("options", JsonArray().apply { add("Cotton") })
        }
        val sut = WCProductModel().copy(attributes = JsonArray().apply { add(attribute) }.toString())

        // WHEN
        val result = sut.getAttributeList().single()

        // THEN
        assertThat(result.variation).isFalse()
        assertThat(result.visible).isFalse()
        assertThat(result.position).isEqualTo(2)
    }

    @Test
    fun `given an attribute, when converted to json, then flags and position are included`() {
        // GIVEN
        val attribute = WCProductModel.ProductAttribute(
            id = 0L,
            name = "Material",
            variation = false,
            visible = false,
            options = listOf("Cotton"),
            position = 2
        )

        // WHEN
        val json = attribute.toJson()

        // THEN
        assertThat(json.get("variation").asBoolean).isFalse()
        assertThat(json.get("visible").asBoolean).isFalse()
        assertThat(json.get("position").asInt).isEqualTo(2)
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
