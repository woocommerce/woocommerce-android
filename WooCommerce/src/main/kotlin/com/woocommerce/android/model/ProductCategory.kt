package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.model.Product.Category
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductCategoryModel

@Parcelize
data class ProductCategory(
    val remoteId: Long,
    val name: String,
    val slug: String,
    val parentId: Long
) : Parcelable {
    fun toCategory(): Category {
        return Category(
            this.remoteId,
            this.name,
            this.slug
        )
    }
}

fun WCProductCategoryModel.toAppProductCategoryModel(): ProductCategory {
    return ProductCategory(
        remoteId = this.remoteCategoryId,
        name = this.name,
        slug = this.slug,
        parentId = this.parent
    )
}
