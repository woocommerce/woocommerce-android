package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.network.Response

class ProductCategoryApiResponse : Response {
    val id: Long = 0L
    var name: String? = null
    var slug: String? = null
    var parent: Long? = null
    val error: JsonObject? = null

    fun asProductCategoryModel(): WCProductCategoryModel {
        val response = this
        return WCProductCategoryModel().apply {
            remoteCategoryId = response.id
            name = response.name ?: ""
            slug = response.slug ?: ""
            parent = response.parent ?: 0L
        }
    }
}

data class ProductCategoryBatchApiResponse(
    @SerializedName("create")
    val createdCategories: List<ProductCategoryApiResponse> = emptyList()
)
