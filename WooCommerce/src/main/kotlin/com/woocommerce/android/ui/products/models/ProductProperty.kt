package com.woocommerce.android.ui.products.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.models.ProductProperty.Type.COMPLEX_PROPERTY
import com.woocommerce.android.ui.products.models.ProductProperty.Type.DIVIDER
import com.woocommerce.android.ui.products.models.ProductProperty.Type.EDITABLE
import com.woocommerce.android.ui.products.models.ProductProperty.Type.LINK
import com.woocommerce.android.ui.products.models.ProductProperty.Type.PROPERTY
import com.woocommerce.android.ui.products.models.ProductProperty.Type.PROPERTY_GROUP
import com.woocommerce.android.ui.products.models.ProductProperty.Type.RATING_BAR
import com.woocommerce.android.ui.products.models.ProductProperty.Type.READ_MORE
import com.woocommerce.android.ui.products.models.ProductProperty.Type.SWITCH
import com.woocommerce.android.ui.products.models.ProductProperty.Type.WARNING

sealed class ProductProperty(val type: Type) {
    enum class Type {
        DIVIDER,
        PROPERTY,
        COMPLEX_PROPERTY,
        RATING_BAR,
        EDITABLE,
        PROPERTY_GROUP,
        LINK,
        READ_MORE,
        SWITCH,
        WARNING
    }

    object Divider : ProductProperty(DIVIDER)

    data class Property(
        @StringRes val title: Int,
        val value: String,
        val isDividerVisible: Boolean = true
    ) : ProductProperty(PROPERTY) {
        override fun isNotEmpty(): Boolean {
            return this.value.isNotBlank()
        }
    }

    data class RatingBar(
        @StringRes val title: Int,
        val value: String,
        val rating: Float
    ) : ProductProperty(RATING_BAR) {
        override fun isNotEmpty(): Boolean {
            return this.value.isNotBlank()
        }
    }

    data class ComplexProperty(
        @StringRes val title: Int? = null,
        val value: String,
        @DrawableRes val icon: Int? = null,
        val showTitle: Boolean = true,
        val maxLines: Int = 1,
        val onClick: (() -> Unit)? = null
    ) : ProductProperty(COMPLEX_PROPERTY) {
        override fun isNotEmpty(): Boolean {
            return value.isNotBlank()
        }
    }

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
        val isDividerVisible: Boolean = true,
        val isHighlighted: Boolean = false,
        @StringRes val propertyFormat: Int = R.string.product_property_default_formatter,
        val onClick: (() -> Unit)? = null
    ) : ProductProperty(PROPERTY_GROUP) {
        override fun isNotEmpty(): Boolean {
            return this.properties.filter { it.value.isNotBlank() }.isNotEmpty()
        }
    }

    data class Switch(
        @StringRes val title: Int,
        val isOn: Boolean,
        @DrawableRes val icon: Int? = null,
        val onStateChanged: ((Boolean) -> Unit)? = null
    ) : ProductProperty(SWITCH)

    data class Warning(
        val content: String = ""
    ) : ProductProperty(WARNING)

    open fun isNotEmpty(): Boolean {
        return true
    }
}
