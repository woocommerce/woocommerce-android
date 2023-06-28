package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyButtonView
import com.woocommerce.android.ui.products.models.ProductProperty.Button

class ButtonViewHolder(parent: ViewGroup) : ProductPropertyViewHolder(
    parent,
    R.layout.product_property_button_view
) {
    fun bind(button: Button) {
        val context = itemView.context
        val buttonView = itemView as WCProductPropertyButtonView

        val text = context.getString(button.text)
        val icon = button.icon?.let { AppCompatResources.getDrawable(context, it) }
//        val tooltipTitle = button.tooltip?.title?.let { context.getString(it) }
//        val tooltipText = button.tooltip?.text?.let { context.getString(it) }
//        val tooltipIcon = button.tooltip?.icon?.let { AppCompatResources.getDrawable(context, it) }

        buttonView.show(text, icon, button.onClick)
    }
}
