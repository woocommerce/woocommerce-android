package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.models.ProductProperty.Link
import com.woocommerce.android.ui.products.propertyviews.WCProductPropertyLinkView

class LinkViewHolder(parent: ViewGroup) : ProductPropertyViewHolder(parent, R.layout.product_property_link_view) {
    fun bind(item: Link) {
        val context = itemView.context
        val linkView = itemView as WCProductPropertyLinkView
        linkView.show(
            title = context.getString(item.title),
            icon = item.icon,
            showDivider = item.isDividerVisible
        )
        item.onClick?.let { onClick ->
            linkView.setOnClickListener {
                onClick()
            }
        }
    }
}
