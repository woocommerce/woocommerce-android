package com.woocommerce.android.ui.products.viewholders

import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyEditableView
import com.woocommerce.android.ui.products.models.ProductDetailItem.Editable

class EditableViewHolder(parent: ViewGroup) : ProductDetailViewHolder(parent, R.layout.product_property_view) {
    fun bind(item: Editable) {
        val context = itemView.context

        val hint = context.getString(item.hint)
        val editableView = View.inflate(
            context,
            R.layout.product_property_editable_view,
            null
        ) as WCProductPropertyEditableView

        editableView.show(hint, item.text)
    }
}
