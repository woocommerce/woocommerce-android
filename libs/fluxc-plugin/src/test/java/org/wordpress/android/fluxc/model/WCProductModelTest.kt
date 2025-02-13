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

        val sut = WCProductModel().apply {
            this.attributes = attributes.toString()
        }

        val result = sut.getAttributeList()

        Assertions.assertThat(result).isEmpty()
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

        val sut = WCProductModel().apply {
            this.attributes = attributes.toString()
        }

        val result = sut.getAttributeList()

        Assertions.assertThat(result).isNotEmpty
    }

    @Test
    fun `isConfigurable should return false when bundledItems is malformed JSON`() {
        val sut = WCProductModel().apply {
            type = CoreProductType.BUNDLE.value
            this.bundledItems = "malformed: [#ˆ%*(!@*#ˆ%(*!#ˆ(%*!]"
        }

        val result = sut.isConfigurable

        assertThat(result).isFalse
    }
}
