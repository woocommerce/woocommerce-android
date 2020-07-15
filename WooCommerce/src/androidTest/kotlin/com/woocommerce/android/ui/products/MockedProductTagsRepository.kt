package com.woocommerce.android.ui.products

import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.model.toProductTag
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCProductModel.ProductTriplet
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.store.WCProductStore

class MockedProductTagsRepository constructor(
    dispatcher: Dispatcher,
    productStore: WCProductStore,
    selectedSite: SelectedSite
) : ProductTagsRepository(
    dispatcher,
    productStore,
    selectedSite
) {
    var tags: List<ProductTag>? = null

    override var canLoadMoreProductTags: Boolean
        get() = false
        set(value) {}

    override fun getProductTags(): List<ProductTag> {
        return tags ?: generateTestProductTags()
    }

    suspend fun fetchProductTags(pageSize: Int, loadMore: Boolean): List<ProductTag> {
        return generateTestProductTags()
    }

    suspend fun fetchProductTags(): List<ProductTag> {
        return generateTestProductTags()
    }

    private fun generateTestProductTagsList(): List<WCProductTagModel> {
        val tags = ArrayList<WCProductTagModel>()
        with(WCProductTagModel(1)) {
            remoteTagId = 1
            description = ""
            slug = ""
            name = ""
            tags.add(this)
        }
        with(WCProductTagModel(2)) {
            remoteTagId = 2
            description = ""
            slug = ""
            name = ""
            tags.add(this)
        }
        return tags
    }

    private fun generateTestProductTagsTripletList(): List<ProductTriplet> {
        return generateTestProductTagsList().map {
            ProductTriplet(
                it.remoteTagId,
                it.name,
                it.slug
            )
        }
    }

    private fun generateTestProductTags(): List<ProductTag> {
        return generateTestProductTagsList().map { it.toProductTag() }
    }
}
