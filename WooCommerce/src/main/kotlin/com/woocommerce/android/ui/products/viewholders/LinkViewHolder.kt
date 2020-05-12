package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyLinkView
import com.woocommerce.android.ui.products.models.ProductProperty.Link

class LinkViewHolder(parent: ViewGroup) : ProductPropertyViewHolder(parent, R.layout.product_property_link_view) {
    fun bind(item: Link) {
        val context = itemView.context
        val linkView = itemView as WCProductPropertyLinkView
        linkView.show(context.getString(item.title))
        item.onClick?.let { onClick ->
            linkView.setOnClickListener {
                onClick()
            }
        }
    }
}
