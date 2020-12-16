package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.databinding.OrderDetailShippingLabelListBinding
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShippingLabelsAdapter
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShippingLabelsAdapter.OnShippingLabelClickListener
import java.math.BigDecimal

class OrderDetailShippingLabelsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailShippingLabelListBinding.inflate(LayoutInflater.from(ctx), this, true)

    fun updateShippingLabels(
        shippingLabels: List<ShippingLabel>,
        productImageMap: ProductImageMap,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        productClickListener: OrderProductActionListener,
        shippingLabelClickListener: OnShippingLabelClickListener
    ) {
        val viewAdapter = binding.shippingLabelList.adapter as? OrderDetailShippingLabelsAdapter
            ?: OrderDetailShippingLabelsAdapter(
                formatCurrencyForDisplay = formatCurrencyForDisplay,
                productImageMap = productImageMap,
                listener = shippingLabelClickListener,
                productClickListener = productClickListener
            )
        binding.shippingLabelList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = viewAdapter
        }
        viewAdapter.shippingLabels = shippingLabels
    }
}
