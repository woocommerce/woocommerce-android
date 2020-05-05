package com.woocommerce.android.ui.products.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.COMPLEX_PROPERTY
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.DIVIDER
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.EDITABLE
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.LINK
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.PROPERTY
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.PROPERTY_GROUP
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.RATING_BAR
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.READ_MORE

sealed class ProductDetailItem(val type: Type) {
    enum class Type {
        DIVIDER,
        PROPERTY,
        COMPLEX_PROPERTY,
        RATING_BAR,
        EDITABLE,
        PROPERTY_GROUP,
        LINK,
        READ_MORE
    }

    object Divider : ProductDetailItem(DIVIDER)

    data class Property(
        @StringRes val title: Int,
        val value: String
    ) : ProductDetailItem(PROPERTY)

    data class RatingBar(
        @StringRes val title: Int,
        val value: String,
        val rating: Float
    ) : ProductDetailItem(RATING_BAR)

    data class ComplexProperty(
        @StringRes val title: Int? = null,
        val value: String,
        @DrawableRes val icon: Int? = null,
        val showTitle: Boolean = true,
        val onClick: (() -> Unit)? = null
    ) : ProductDetailItem(COMPLEX_PROPERTY)

    data class Link(
        @StringRes val title: Int,
        val onClick: (() -> Unit)
    ) : ProductDetailItem(LINK)

    data class Editable(
        @StringRes val hint: Int,
        val text: String = "",
        val onTextChanged: ((String) -> Unit)? = null
    ) : ProductDetailItem(EDITABLE)

    data class ReadMore(
        @StringRes val caption: Int,
        val content: String = "",
        val maxLines: Int = 2
    ) : ProductDetailItem(READ_MORE)

    data class PropertyGroup(
        @StringRes val title: Int,
        val properties: Map<String, String>,
        @DrawableRes val icon: Int? = null,
        val showTitle: Boolean = true,
        val onClick: (() -> Unit)? = null
    ) : ProductDetailItem(PROPERTY_GROUP)
}
