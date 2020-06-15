package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductCategoryModel

@Parcelize
data class ProductCategory(
    val remoteId: Long,
    val name: String,
    val slug: String,
    val parentId: Long? = 0L
) : Parcelable {
    fun toProductCategory(): ProductCategory {
        return ProductCategory(
            this.remoteId,
            this.name,
            this.slug
        )
    }
}

fun WCProductCategoryModel.toProductCategory(): ProductCategory {
    return ProductCategory(
        remoteId = this.remoteCategoryId,
        name = this.name,
        slug = this.slug,
        parentId = this.parent
    )
}
