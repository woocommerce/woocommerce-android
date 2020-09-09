package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.models.ProductProperty.Warning

class WarningViewHolder(
    parent: ViewGroup
) : ProductPropertyViewHolder(parent, R.layout.product_property_warning_layout) {
    fun bind(item: Warning) {
        val content = itemView.findViewById<TextView>(R.id.warningBody)
        content.text = item.content
    }
}
