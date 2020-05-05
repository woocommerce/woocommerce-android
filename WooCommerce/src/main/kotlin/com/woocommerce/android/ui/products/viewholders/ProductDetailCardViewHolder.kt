package com.woocommerce.android.ui.products.viewholders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyCardView
import com.woocommerce.android.ui.products.models.ProductDetailCard

open class ProductDetailCardViewHolder(
    parent: ViewGroup
) : ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.product_property_cardview, parent, false)) {
    fun bind(card: ProductDetailCard) {
        val view = itemView as WCProductPropertyCardView
        view.show(card.caption, card.properties)
    }
}
