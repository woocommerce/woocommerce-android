package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailProductListBinding
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.ViewAddonClickListener
import com.woocommerce.android.ui.orders.details.OrderProduct
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailProductItemListAdapter
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

    @Suppress("LongParameterList")
    fun updateProductItemsList(
        orderProductItems: List<OrderProduct>,
        productImageMap: ProductImageMap,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        productClickListener: OrderProductActionListener,
        onProductMenuItemClicked: () -> Unit,
        onViewAddonsClick: ViewAddonClickListener? = null
    ) {
        val adapter = OrderDetailProductItemListAdapter(
            orderProductItems,
            productImageMap,
            formatCurrencyForDisplay,
            productClickListener,
            onViewAddonsClick
        )
        updateList(
            adapter,
            orderProductItems.size,
            onProductMenuItemClicked
        )
    }

    fun updateProductList(
        orderItems: List<Order.Item>,
        productImageMap: ProductImageMap,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        productClickListener: OrderProductActionListener,
        onProductMenuItemClicked: () -> Unit,
        onViewAddonsClick: ViewAddonClickListener? = null
    ) {
        val adapter = OrderDetailProductListAdapter(
            orderItems,
            productImageMap,
            formatCurrencyForDisplay,
            productClickListener,
            onViewAddonsClick
        )

        updateList(
            adapter,
            orderItems.size,
            onProductMenuItemClicked
        )
    }

    private fun updateList(
        listAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
        size: Int,
        onProductMenuItemClicked: () -> Unit,
    ) {
        binding.productListLblProduct.text = StringUtils.getQuantityString(
            context = context,
            quantity = size,
            default = R.string.orderdetail_product_multiple_uppercase,
            one = R.string.orderdetail_product_uppercase
        )

        binding.productListProducts.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = listAdapter

            if (itemDecorationCount == 0) {
                addItemDecoration(
                    AlignedDividerDecoration(
                        context,
                        DividerItemDecoration.VERTICAL
                    )
                )
            }

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false
        }

        val popupMenu = PopupMenu(context, binding.productListBtnMenu)
        popupMenu.menu.add(0, 0, 0, R.string.orderdetail_products_recreate_shipping_label_menu)
        popupMenu.menu.findItem(0).setOnMenuItemClickListener {
            onProductMenuItemClicked()
            true
        }

        binding.productListBtnMenu.setOnClickListener {
            popupMenu.show()
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

    fun showProductListMenuButton(isVisible: Boolean) {
        binding.productListBtnMenu.isVisible = isVisible
    }
}
