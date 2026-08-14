package com.woocommerce.android.ui.products.filter

import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProductFilterHistoryMapperTest {
    private val sut = ProductFilterHistoryMapper(Gson())

    @Test
    fun `given a full selection, when encoding then decoding, then it round-trips`() {
        val filter = ProductFilterResult(
            stockStatus = "instock",
            productType = "simple",
            productStatus = "draft",
            productCategory = "5",
            productCategoryName = "Shoes"
        )

        assertThat(sut.fromPayload(sut.toPayload(filter))).isEqualTo(filter)
    }

    @Test
    fun `given identical selections, when encoded, then payloads are identical`() {
        val a = ProductFilterResult("instock", null, "draft", null, null)
        val b = ProductFilterResult("instock", null, "draft", null, null)

        assertThat(sut.toPayload(a)).isEqualTo(sut.toPayload(b))
    }

    @Test
    fun `given an invalid payload, when decoding, then null is returned`() {
        assertThat(sut.fromPayload("not-json")).isNull()
    }

    @Test
    fun `given an empty json payload, when decoding, then a filter with no selection is returned`() {
        assertThat(sut.fromPayload("{}")).isEqualTo(ProductFilterResult(null, null, null, null, null))
    }

    @Test
    fun `given a category name without a category id, when encoding, then the name is dropped`() {
        val withStaleName = ProductFilterResult("instock", null, null, null, "Any")
        val withoutName = ProductFilterResult("instock", null, null, null, null)

        // The stale name must not leak into the payload, otherwise it breaks dedup against the same
        // stock-only filter saved from another path.
        assertThat(sut.toPayload(withStaleName)).isEqualTo(sut.toPayload(withoutName))
        assertThat(sut.fromPayload(sut.toPayload(withStaleName))?.productCategoryName).isNull()
    }

    @Test
    fun `given a category name with a category id, when encoding, then the name is kept`() {
        val filter = ProductFilterResult(null, null, null, "5", "Shoes")

        assertThat(sut.fromPayload(sut.toPayload(filter))?.productCategoryName).isEqualTo("Shoes")
    }
}
