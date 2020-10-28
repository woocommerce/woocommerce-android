package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailProductListAdapter
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.widgets.AlignedDividerDecoration
import kotlinx.android.synthetic.main.order_detail_product_list.view.*
import java.math.BigDecimal

class OrderDetailProductListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_product_list, this)
    }

    fun updateProductList(
        orderItems: List<Order.Item>,
        productImageMap: ProductImageMap,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        productClickListener: OrderProductActionListener
    ) {
        productList_lblProduct.text = StringUtils.getQuantityString(
            context,
            orderItems.size,
            R.string.orderdetail_product_multiple,
            R.string.orderdetail_product
        )

        productList_products.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = OrderDetailProductListAdapter(
                orderItems, productImageMap, formatCurrencyForDisplay, productClickListener
            )

            if (itemDecorationCount == 0) {
                addItemDecoration(
                    AlignedDividerDecoration(
                        context,
                        DividerItemDecoration.VERTICAL,
                        R.id.productInfo_name,
                        padding = context.resources.getDimensionPixelSize(dimen.major_100)
                    )
                )
            }

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false
        }
    }

    fun showMarkOrderCompleteButton(
        isVisible: Boolean,
        onMarkOrderCompleteButtonTapped: () -> Unit
    ) {
        productList_btnMarkOrderComplete.isVisible = isVisible
        productList_btnMarkOrderComplete.setOnClickListener { onMarkOrderCompleteButtonTapped() }
    }
}
