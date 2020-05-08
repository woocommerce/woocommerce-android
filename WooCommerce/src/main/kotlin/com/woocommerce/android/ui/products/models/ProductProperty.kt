package com.woocommerce.android.ui.products.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.ui.products.models.ProductProperty.Type.COMPLEX_PROPERTY
import com.woocommerce.android.ui.products.models.ProductProperty.Type.DIVIDER
import com.woocommerce.android.ui.products.models.ProductProperty.Type.EDITABLE
import com.woocommerce.android.ui.products.models.ProductProperty.Type.LINK
import com.woocommerce.android.ui.products.models.ProductProperty.Type.PROPERTY
import com.woocommerce.android.ui.products.models.ProductProperty.Type.PROPERTY_GROUP
import com.woocommerce.android.ui.products.models.ProductProperty.Type.RATING_BAR
import com.woocommerce.android.ui.products.models.ProductProperty.Type.READ_MORE

sealed class ProductProperty(val type: Type) {
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

    object Divider : ProductProperty(DIVIDER)

    data class Property(
        @StringRes val title: Int,
        val value: String
    ) : ProductProperty(PROPERTY)

    data class RatingBar(
        @StringRes val title: Int,
        val value: String,
        val rating: Float
    ) : ProductProperty(RATING_BAR)

    data class ComplexProperty(
        @StringRes val title: Int? = null,
        val value: String,
        @DrawableRes val icon: Int? = null,
        val showTitle: Boolean = true,
        val onClick: (() -> Unit)? = null
    ) : ProductProperty(COMPLEX_PROPERTY)

    data class Link(
        @StringRes val title: Int,
        val onClick: (() -> Unit)?
    ) : ProductProperty(LINK)

    data class Editable(
        @StringRes val hint: Int,
        val text: String = "",
        var shouldFocus: Boolean = false,
        val onTextChanged: ((String) -> Unit)? = null
    ) : ProductProperty(EDITABLE)

    data class ReadMore(
        @StringRes val caption: Int,
        val content: String = "",
        val maxLines: Int = 2
    ) : ProductProperty(READ_MORE)

    data class PropertyGroup(
        @StringRes val title: Int,
        val properties: Map<String, String>,
        @DrawableRes val icon: Int? = null,
        val showTitle: Boolean = true,
        val onClick: (() -> Unit)? = null
    ) : ProductProperty(PROPERTY_GROUP)
}
