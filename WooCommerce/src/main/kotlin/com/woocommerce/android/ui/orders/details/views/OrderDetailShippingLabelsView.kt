package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShippingLabelsAdapter
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShippingLabelsAdapter.OnShippingLabelClickListener
import kotlinx.android.synthetic.main.order_detail_shipping_label_list.view.*
import java.math.BigDecimal

class OrderDetailShippingLabelsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_shipping_label_list, this)
    }

    fun updateShippingLabels(
        shippingLabels: List<ShippingLabel>,
        productImageMap: ProductImageMap,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        productClickListener: OrderProductActionListener,
        shippingLabelClickListener: OnShippingLabelClickListener
    ) {
        val viewAdapter = shippingLabel_list.adapter as? OrderDetailShippingLabelsAdapter
            ?: OrderDetailShippingLabelsAdapter(
                formatCurrencyForDisplay = formatCurrencyForDisplay,
                productImageMap = productImageMap,
                listener = shippingLabelClickListener,
                productClickListener = productClickListener
            )
        shippingLabel_list.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = viewAdapter
        }
        viewAdapter.shippingLabels = shippingLabels
    }
}
