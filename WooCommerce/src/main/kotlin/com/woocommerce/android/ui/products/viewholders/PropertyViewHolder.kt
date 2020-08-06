package com.woocommerce.android.ui.products.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyView
import com.woocommerce.android.ui.products.models.ProductProperty.Property

class PropertyViewHolder(parent: ViewGroup) : ProductPropertyViewHolder(parent, R.layout.product_property_view) {
    fun bind(item: Property) {
        val context = itemView.context
        val propertyView = itemView as WCProductPropertyView
        propertyView.show(LinearLayout.HORIZONTAL, context.getString(item.title), item.value)

        val divider = propertyView.findViewById<View>(R.id.divider)
        divider.isVisible = item.isDividerVisible
    }
}
