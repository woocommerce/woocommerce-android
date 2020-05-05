package com.woocommerce.android.ui.products.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.COMPLEX_PROPERTY
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.DIVIDER
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.EDITABLE
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.PROPERTY
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.PROPERTY_GROUP
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.RATING_BAR

sealed class ProductDetailItem(val type: Type) {
    enum class Type {
        DIVIDER,
        PROPERTY,
        COMPLEX_PROPERTY,
        RATING_BAR,
        EDITABLE,
        PROPERTY_GROUP
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
        val value: String,
        @StringRes val title: Int? = null,
        @DrawableRes val icon: Int? = null,
        val onClick: (() -> Unit)? = null
    ) : ProductDetailItem(COMPLEX_PROPERTY)

    data class Editable(
        @StringRes val hint: Int,
        val text: String = "",
        val onTextChanged: ((String) -> Unit)? = null
    ) : ProductDetailItem(EDITABLE)

    data class PropertyGroup(
        @StringRes val title: Int,
        val properties: Map<Int?, String>,
        @DrawableRes val icon: Int? = null,
        val onClick: (() -> Unit)? = null
    ) : ProductDetailItem(PROPERTY_GROUP)
}
