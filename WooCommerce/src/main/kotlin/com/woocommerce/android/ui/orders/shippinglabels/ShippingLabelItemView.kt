package com.woocommerce.android.ui.orders.shippinglabels

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.view_shipping_label_item.view.*

class ShippingLabelItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    init {
        View.inflate(context, R.layout.view_shipping_label_item, this)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ShippingLabelItemView
            )
            try {
                // Set the shipping label icon
                val drawableId = a.getResourceId(R.styleable.ShippingLabelItemView_shippingLabelItemIcon, 0)
                if (drawableId != 0) {
                    shippingLabelIcon.setImageResource(drawableId)
                }

                // Set the shipping label title
                a.getString(R.styleable.ShippingLabelItemView_shippingLabelItemTitle)?.let {
                    shippingLabelTitle.text = it
                }

                // Set the shipping label value
                a.getString(R.styleable.ShippingLabelItemView_shippingLabelItemValue)?.let {
                    shippingLabelValue.text = it
                }

                // show/hide shipment tracking button
                val showTrackingBtn =
                a.getBoolean(R.styleable.ShippingLabelItemView_showTrackShipmentButton, false)
                shippingLabelItem_btnTrack.isVisible = showTrackingBtn
            } finally {
                a.recycle()
            }
        }
    }

    fun setShippingLabelTitle(title: String) {
        shippingLabelTitle.text = title
    }

    fun setShippingLabelValue(value: String) {
        shippingLabelValue.text = value
    }

    fun showTrackingItemButton(show: Boolean) {
        shippingLabelItem_btnTrack.isVisible = show
    }

    fun setTrackingItemClickListener(clickListener: (() -> Unit)) {
        shippingLabelItem_btnTrack.setOnClickListener { clickListener.invoke() }
    }

    fun getTrackingItemButton(): View = shippingLabelItem_btnTrack
}
