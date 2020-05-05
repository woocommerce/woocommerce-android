package com.woocommerce.android.ui.products.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyView
import com.woocommerce.android.ui.products.models.ProductDetailItem.PropertyGroup

class PropertyGroupViewHolder(parent: ViewGroup) : ProductDetailViewHolder(parent, R.layout.product_property_view) {
    fun bind(item: PropertyGroup) {
        val context = itemView.context

        val propertyView = View.inflate(context, R.layout.product_property_view, null) as WCProductPropertyView
        propertyView.show(LinearLayout.VERTICAL, context.getString(item.title), getPropertyValue(item.properties), item.icon)
        propertyView.setMaxLines(Integer.MAX_VALUE)
        propertyView.showPropertyName(true)

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
        properties: Map<Int?, String>,
        @StringRes propertyValueFormatterId: Int = R.string.product_property_default_formatter
    ): String {
        var propertyValue = ""
        properties.forEach { property ->
            if (property.key == null) {
                propertyValue += property.value
            } else if (property.value.isNotEmpty()) {
                if (propertyValue.isNotEmpty()) {
                    propertyValue += "\n"
                }
                val name = itemView.context.getString(property.key!!)
                propertyValue += itemView.context.getString(propertyValueFormatterId, name, property.value)
            }
        }
        return propertyValue
    }
}
