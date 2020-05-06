package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyEditableView
import com.woocommerce.android.ui.products.models.ProductDetailItem.Editable

class EditableViewHolder(parent: ViewGroup) : ProductDetailPropertyViewHolder(parent, R.layout.product_property_editable_view) {
    fun bind(item: Editable) {
        val context = itemView.context
        val hint = context.getString(item.hint)
        val editableView = itemView as WCProductPropertyEditableView

        item.onTextChanged?.let { onTextChanged ->
            editableView.setOnTextChangedListener { text -> onTextChanged(text.toString()) }
        }

        editableView.show(hint, item.text)
    }
}
