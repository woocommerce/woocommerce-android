package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.models.ProductProperty.RatingBar
import com.woocommerce.android.ui.products.propertyviews.WCProductPropertyView

class RatingBarViewHolder(parent: ViewGroup) : ProductPropertyViewHolder(parent, R.layout.product_property_view) {
    fun bind(item: RatingBar) {
        val context = itemView.context
        val propertyView = itemView as WCProductPropertyView
        propertyView.show(
            orientation = LinearLayout.VERTICAL,
            caption = context.getString(item.title),
            detail = item.value,
            showTitle = true,
            propertyIcon = item.icon,
            isRating = true
        )
        propertyView.setRating(item.rating)

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
}
