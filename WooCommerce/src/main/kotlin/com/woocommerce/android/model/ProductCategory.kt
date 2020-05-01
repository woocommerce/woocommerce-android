package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductCategoryModel

@Parcelize
data class ProductCategory(
    val remoteId: Long,
    val name: String,
    val slug: String,
    val parent: Long
) : Parcelable
{
    fun isSameCategory(productCategory: ProductCategory): Boolean {
        return remoteId == productCategory.remoteId &&
                name == productCategory.name &&
                slug == productCategory.slug &&
                parent == productCategory.parent
    }
}

fun WCProductCategoryModel.toAppModel(): ProductCategory {
    return ProductCategory(
            remoteId = this.remoteCategoryId,
            name = name,
            slug = this.slug,
            parent = this.parent
    )
}
