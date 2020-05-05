package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyLinkView
import com.woocommerce.android.ui.products.models.ProductDetailItem.Link

class LinkViewHolder(parent: ViewGroup) : ProductDetailPropertyViewHolder(parent, R.layout.product_property_link_view) {
    fun bind(item: Link) {
        val context = itemView.context
        val linkView = itemView as WCProductPropertyLinkView
        linkView.show(context.getString(item.title))
        linkView.setOnClickListener {
            item.onClick()
            /*
                // TODO: Move to ViewModel
                view.setOnClickListener {
                    AnalyticsTracker.track(tracksEvent)
                    ChromeCustomTabUtils.launchUrl(context, url)
                }
             */
        }
    }
}
