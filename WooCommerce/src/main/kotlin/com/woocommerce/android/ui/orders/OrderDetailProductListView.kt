package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_DETAIL_PRODUCT_TAPPED
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.widgets.AlignedDividerDecoration
import kotlinx.android.synthetic.main.order_detail_product_list.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

class OrderDetailProductListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_product_list, this)
    }
    private lateinit var viewAdapter: ProductListAdapter
    private var isExpanded = false

    /**
     * Initialize and format this view.
     *
     * @param [orderModel] The order containing the product list to display.
     * @param [productImageMap] Images for products.
     * @param [expanded] If true, expanded view will be shown, else collapsed view.
     * @param [formatCurrencyForDisplay] Function to use for formatting currencies for display.
     * @param [orderListener] Listener for routing order click actions. If null, the buttons will be hidden.
     * @param [productListener] Listener for routing product click actions.
     * @param [refunds] List of refunds order the order.
     */
    fun initView(
        orderModel: WCOrderModel,
        productImageMap: ProductImageMap,
        expanded: Boolean,
        formatCurrencyForDisplay: (BigDecimal) -> String,
        orderListener: OrderActionListener? = null,
        productListener: OrderProductActionListener? = null,
        refunds: List<Refund>
    ) {
        isExpanded = expanded

        val viewManager = androidx.recyclerview.widget.LinearLayoutManager(context)

        val order = orderModel.toAppModel()
        val leftoverProducts = order.getMaxRefundQuantities(refunds).filter { it.value > 0 }
        val filteredItems = order.items.filter { leftoverProducts.contains(it.uniqueId) }
                .map {
                    val newQuantity = leftoverProducts[it.uniqueId]
                    it.copy(
                            quantity = newQuantity ?: error("Missing product"),
                            total = it.price.times(newQuantity.toBigDecimal()),
                            totalTax = it.totalTax.divide(it.quantity.toBigDecimal(), 2, HALF_UP)
                    )
                }

        viewAdapter = ProductListAdapter(
                filteredItems,
                productImageMap,
                formatCurrencyForDisplay,
                isExpanded,
                productListener
        )

        productList_products.apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            adapter = viewAdapter

            if (itemDecorationCount == 0) {
                addItemDecoration(
                    AlignedDividerDecoration(
                        context,
                        DividerItemDecoration.VERTICAL,
                        R.id.productInfo_name,
                        clipToMargin = false
                    )
                )
            }

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false
        }

        updateView(orderModel, orderListener)
    }

    fun updateView(orderModel: WCOrderModel, orderListener: OrderActionListener? = null) {
        orderListener?.let {
            if (orderModel.status == CoreOrderStatus.PROCESSING.value) {
                productList_btnFulfill.visibility = View.VISIBLE
                productList_btnDetails.visibility = View.GONE
                productList_btnDetails.setOnClickListener(null)
                productList_btnFulfill.setOnClickListener {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_FULFILL_ORDER_BUTTON_TAPPED)
                    orderListener.openOrderFulfillment(orderModel)
                }
            } else {
                productList_btnFulfill.visibility = View.GONE
                productList_btnDetails.visibility = View.VISIBLE
                productList_btnDetails.setOnClickListener {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_PRODUCT_DETAIL_BUTTON_TAPPED)
                    orderListener.openOrderProductList(orderModel)
                }
                productList_btnFulfill.setOnClickListener(null)
            }
        } ?: hideButtons()
    }

    // called when a product is fetched to ensure we show the correct product image
    fun refreshProductImages() {
        if (::viewAdapter.isInitialized) {
            viewAdapter.notifyDataSetChanged()
        }
    }

    private fun hideButtons() {
        productList_btnFulfill.setOnClickListener(null)
        productList_btnDetails.setOnClickListener(null)
        productList_btnFulfill.visibility = View.GONE
        productList_btnDetails.visibility = View.GONE
    }

    class ProductListAdapter(
        private val orderItems: List<Order.Item>,
        private val productImageMap: ProductImageMap,
        private val formatCurrencyForDisplay: (BigDecimal) -> String,
        private var isExpanded: Boolean,
        private val productListener: OrderProductActionListener?
    ) : RecyclerView.Adapter<ProductListAdapter.ViewHolder>() {
        class ViewHolder(val view: OrderDetailProductItemView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: OrderDetailProductItemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.order_detail_product_list_item, parent, false)
                    as OrderDetailProductItemView
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = orderItems[position]
            val productId = ProductHelper.productOrVariationId(item.productId, item.variationId)
            val productImage = productImageMap.get(productId)
            holder.view.initView(orderItems[position], productImage, isExpanded, formatCurrencyForDisplay)
            holder.view.setOnClickListener {
                AnalyticsTracker.track(ORDER_DETAIL_PRODUCT_TAPPED)
                productListener?.openOrderProductDetail(productId)
            }
        }

        override fun getItemCount() = orderItems.size
    }
}
