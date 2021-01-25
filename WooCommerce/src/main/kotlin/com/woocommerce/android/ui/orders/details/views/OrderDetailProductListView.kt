package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.databinding.OrderDetailProductListBinding
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailProductListAdapter
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.widgets.AlignedDividerDecoration
import java.math.BigDecimal

class OrderDetailProductListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailProductListBinding.inflate(LayoutInflater.from(ctx), this)

    fun updateProductList(
        orderItems: List<Order.Item>,
        productImageMap: ProductImageMap,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        productClickListener: OrderProductActionListener
    ) {
        binding.productListLblProduct.text = StringUtils.getQuantityString(
            context = context,
            quantity = orderItems.size,
            default = R.string.orderdetail_product_multiple,
            one = R.string.orderdetail_product
        )

        binding.productListProducts.apply {
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

    fun notifyProductChanged(remoteProductId: Long) {
        with(binding.productListProducts.adapter as? OrderDetailProductListAdapter) {
            this?.notifyProductChanged(remoteProductId)
        }
    }

    fun showMarkOrderCompleteButton(
        isVisible: Boolean,
        onMarkOrderCompleteButtonTapped: () -> Unit
    ) {
        binding.productListBtnMarkOrderComplete.isVisible = isVisible
        binding.productListBtnMarkOrderComplete.setOnClickListener { onMarkOrderCompleteButtonTapped() }
    }

    fun showCreateShippingLabelButton(
        isVisible: Boolean,
        onCreateShippingLabelButtonTapped: () -> Unit,
        onShippingLabelNoticeTapped: () -> Unit
    ) {
        binding.productListBtnCreateShippingLabel.isVisible = isVisible
        binding.productListBtnCreateShippingLabel.setOnClickListener { onCreateShippingLabelButtonTapped() }

        binding.productListShippingLabelsNotice.isVisible = isVisible
        binding.productListShippingLabelsNotice.setOnClickListener { onShippingLabelNoticeTapped() }
    }
}
