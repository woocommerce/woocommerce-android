package com.woocommerce.android.ui.orders.shippinglabels

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ViewShippingLabelItemBinding

class ShippingLabelItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private val binding = ViewShippingLabelItemBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.ShippingLabelItemView
            )
            try {
                // Set the shipping label icon
                val drawableId = a.getResourceId(R.styleable.ShippingLabelItemView_shippingLabelItemIcon, 0)
                if (drawableId != 0) {
                    binding.shippingLabelIcon.setImageResource(drawableId)
                }

                // Set the shipping label title
                a.getString(R.styleable.ShippingLabelItemView_shippingLabelItemTitle)?.let {
                    binding.shippingLabelTitle.text = it
                }

                // Set the shipping label value
                a.getString(R.styleable.ShippingLabelItemView_shippingLabelItemValue)?.let {
                    binding.shippingLabelValue.text = it
                }

                // show/hide shipment tracking button
                val showTrackingBtn =
                a.getBoolean(R.styleable.ShippingLabelItemView_showTrackShipmentButton, false)
                binding.shippingLabelItemBtnTrack.isVisible = showTrackingBtn
            } finally {
                a.recycle()
            }
        }
    }

    fun setShippingLabelTitle(title: String) {
        binding.shippingLabelTitle.text = title
    }

    fun setShippingLabelValue(value: String) {
        binding.shippingLabelValue.text = value
    }

    fun showTrackingItemButton(show: Boolean) {
        binding.shippingLabelItemBtnTrack.isVisible = show
    }

    fun setTrackingItemClickListener(clickListener: (() -> Unit)) {
        binding.shippingLabelItemBtnTrack.setOnClickListener { clickListener.invoke() }
    }

    fun getTrackingItemButton(): View = binding.shippingLabelItemBtnTrack
}
