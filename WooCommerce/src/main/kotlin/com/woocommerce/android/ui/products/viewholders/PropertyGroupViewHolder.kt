package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyView
import com.woocommerce.android.ui.products.models.ProductProperty.PropertyGroup

class PropertyGroupViewHolder(parent: ViewGroup) : ProductPropertyViewHolder(parent, R.layout.product_property_view) {
    fun bind(item: PropertyGroup) {
        val context = itemView.context

        val propertyView = itemView as WCProductPropertyView
        propertyView.show(
            LinearLayout.VERTICAL,
            context.getString(item.title),
            getPropertyValue(item.properties),
            item.icon
        )
        propertyView.setMaxLines(Integer.MAX_VALUE)
        propertyView.showPropertyName(item.showTitle)

        if (item.onClick != null) {
            item.onClick.let { onClick ->
                propertyView.setClickListener {
                    onClick()
                }
            }
        } else {
            propertyView.removeClickListener()
        }
    }

    private fun getPropertyValue(
        properties: Map<String, String>,
        @StringRes propertyValueFormatterId: Int = R.string.product_property_default_formatter
    ): String {
        var propertyValue = ""
        properties.forEach { property ->
            if (property.key.isEmpty()) {
                propertyValue += property.value
            } else if (property.value.isNotEmpty()) {
                if (propertyValue.isNotEmpty()) {
                    propertyValue += "\n"
                }
                propertyValue += itemView.context.getString(propertyValueFormatterId, property.key, property.value)
            }
        }
        return propertyValue
    }
}
