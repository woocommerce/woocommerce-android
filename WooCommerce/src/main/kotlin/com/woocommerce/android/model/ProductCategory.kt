package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.model.Product.Category
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductCategoryModel

@Parcelize
data class ProductCategory(
    val remoteCategoryId: Long,
    val name: String,
    val slug: String,
    val parentId: Long
) : Parcelable {
    fun toCategory(): Category {
        return Category(
            this.remoteCategoryId,
            this.name,
            this.slug
        )
    }

    fun isSameCategory(category: ProductCategory): Boolean {
        return remoteCategoryId == category.remoteCategoryId &&
            name == category.name &&
            slug == category.slug &&
            parentId == category.parentId
    }
}

fun WCProductCategoryModel.toAppProductCategoryModel(): ProductCategory {
    return ProductCategory(
        remoteCategoryId = this.remoteCategoryId,
        name = this.name,
        slug = this.slug,
        parentId = this.parent
    )
}
