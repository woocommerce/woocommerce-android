package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.models.ProductProperty.ReadMore
import com.woocommerce.android.ui.products.propertyviews.WCProductPropertyReadMoreView
import org.wordpress.android.util.HtmlUtils

class ReadMoreViewHolder(
    parent: ViewGroup
) : ProductPropertyViewHolder(parent, R.layout.product_property_read_more_view) {
    fun bind(item: ReadMore) {
        val context = itemView.context
        val propertyView = itemView as WCProductPropertyReadMoreView
        propertyView.show(
            context.getString(item.caption),
            HtmlUtils.fastStripHtml(item.content),
            item.maxLines
        )
    }
}
