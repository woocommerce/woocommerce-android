package com.woocommerce.android.model

import com.woocommerce.android.ui.products.ProductTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel

class ProductVariationTest {
    @Test
    fun `given mixed menu orders, when sorting, then variations are sorted by menu order with ties newest first`() {
        // GIVEN
        val variations = listOf(
            variation(variationId = 1, menuOrder = 1),
            variation(variationId = 2, menuOrder = 2),
            variation(variationId = 3, menuOrder = 2),
            variation(variationId = 4, menuOrder = 1),
        )

        // WHEN
        val sorted = variations.sorted()

        // THEN
        assertThat(sorted.map { it.remoteVariationId }).containsExactly(4L, 1L, 3L, 2L)
    }

    @Test
    fun `given a description with tags around text, when mapped to the app model, then the tags are stripped`() {
        val model = variationModel(description = "<p>Soft <strong>cotton</strong></p>")

        assertThat(model.toAppModel().description).isEqualTo("Soft cotton")
        assertThat(SubscriptionProductVariation(model).description).isEqualTo("Soft cotton")
    }

    @Test
    fun `given a description made only of tags, when the variation is mapped back, then it is unchanged`() {
        val model = variationModel(description = "<img src=\"photo.jpg\">")

        val updatedModel = model.toAppModel().copy(sku = "new-sku").toDataModel(model)

        assertThat(updatedModel.description).isEqualTo(model.description)
    }

    @Test
    fun `given a subscription variation description made only of tags, when mapped back, then it is unchanged`() {
        val model = variationModel(description = "<img src=\"photo.jpg\">")

        val updatedModel = SubscriptionProductVariation(model).copy(sku = "new-sku").toDataModel(model)

        assertThat(updatedModel.description).isEqualTo(model.description)
    }

    private fun variationModel(description: String) = WCProductVariationModel(LocalId(2)).copy(
        dateCreated = "2018-01-05T05:14:30Z",
        localSiteId = LocalId(1),
        remoteProductId = RemoteId(1),
        remoteVariationId = RemoteId(1),
        description = description,
    )

    private fun variation(variationId: Long, menuOrder: Int) =
        ProductTestUtils.generateProductVariation(variationId = variationId).copy(menuOrder = menuOrder)
}
