package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductModel

/**
 * Represents a local attribute, which is an attribute assigned to a specific product
 */
@Parcelize
data class ProductAttribute(
    val id: Long,
    val name: String,
    val options: List<String>,
    val isVisible: Boolean
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        return (other as? ProductAttribute)?.let {
            id == it.id &&
                name == it.name &&
                options == it.options &&
                isVisible == it.isVisible
        } ?: false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    fun getCommaSeparatedOptions(): String {
        if (options.isEmpty()) return ""
        var commaSeparatedOptions = ""
        options.forEach { option ->
            if (commaSeparatedOptions.isEmpty()) {
                commaSeparatedOptions = option
            } else {
                commaSeparatedOptions += ", $option"
            }
        }
        return commaSeparatedOptions
    }

    fun isLocalAttribute() = id == 0L
}

fun WCProductModel.ProductAttribute.toAppModel(): ProductAttribute {
    return ProductAttribute(
        id = this.id,
        name = this.name,
        options = this.options,
        isVisible = this.visible
    )
}
