package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToMMMddYYYYhhmm
import com.woocommerce.android.extensions.getCountryLabelByCountryCode
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.loadProductItems
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.util.AddressUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.AppRatingDialog
import kotlinx.android.synthetic.main.order_detail_shipping_label_list.view.*
import kotlinx.android.synthetic.main.order_detail_shipping_label_list_item.view.*
import java.math.BigDecimal

class OrderDetailShippingLabelListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_shipping_label_list, this)
    }

    private lateinit var viewAdapter: ShippingLabelListAdapter

    fun initView(
        order: Order,
        shippingLabels: List<ShippingLabel>,
        productImageMap: ProductImageMap,
        formatCurrencyForDisplay: (BigDecimal) -> String
    ) {
        val viewManager = LinearLayoutManager(context)
        viewAdapter = ShippingLabelListAdapter(
            context,
            productImageMap,
            order.items,
            shippingLabels,
            formatCurrencyForDisplay
        )

        shippingLabel_list.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            itemAnimator = DefaultItemAnimator()
            adapter = viewAdapter

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false
        }
    }

    class ShippingLabelListAdapter(
        private val context: Context,
        private val productImageMap: ProductImageMap,
        private val orderItems: List<Order.Item>,
        private val shippingLabels: List<ShippingLabel>,
        private val formatCurrencyForDisplay: (BigDecimal) -> String
    ) : RecyclerView.Adapter<ShippingLabelListAdapter.ShippingLabelListViewHolder>() {
        private val viewPool = RecyclerView.RecycledViewPool()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShippingLabelListViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.order_detail_shipping_label_list_item, parent, false)
            return ShippingLabelListViewHolder(view)
        }

        override fun onBindViewHolder(holder: ShippingLabelListViewHolder, position: Int) {
            val shippingLabel = shippingLabels[position]
            holder.bindTo(context, shippingLabel, formatCurrencyForDisplay)
            holder.bindProductItems(
                context,
                productImageMap,
                shippingLabel.loadProductItems(orderItems),
                formatCurrencyForDisplay,
                viewPool
            )
        }

        override fun getItemCount() = shippingLabels.size

        class ShippingLabelListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bindTo(
                context: Context,
                shippingLabel: ShippingLabel,
                formatCurrencyForDisplay: (BigDecimal) -> String
            ) {
                itemView.shippingLabelItem_viewMore.setOnClickListener {
                    val isChecked = itemView.shippingLabelItem_viewMoreButtonImage.rotation == 0F
                    if (isChecked) {
                        // TODO: add tracking event
                        itemView.shippingLabelItem_morePanel.expand()
                        itemView.shippingLabelItem_viewMoreButtonImage.animate().rotation(180F).setDuration(200).start()
                        itemView.shippingLabelItem_viewMoreButtonTitle.text = context.getString(
                            R.string.orderdetail_shipping_label_item_hide_shipping
                        )
                    } else {
                        // TODO: add tracking event
                        itemView.shippingLabelItem_morePanel.collapse()
                        itemView.shippingLabelItem_viewMoreButtonImage.animate().rotation(0F).setDuration(200).start()
                        itemView.shippingLabelItem_viewMoreButtonTitle.text = context.getString(
                            R.string.orderdetail_shipping_label_item_show_shipping
                        )
                    }
                }

                itemView.shippingLabelList_lblPackage.text = context.getString(
                    R.string.orderdetail_shipping_label_item_header, adapterPosition + 1
                )

                if (shippingLabel.refund != null) {
                    itemView.shippingLabelItem_trackingNumber.setShippingLabelTitle(context.getString(
                            R.string.orderdetail_shipping_label_refund_title,
                            shippingLabel.serviceName
                        ))

                    itemView.shippingLabelItem_trackingNumber.setShippingLabelValue(
                        context.getString(R.string.orderdetail_shipping_label_refund_subtitle,
                            formatCurrencyForDisplay(shippingLabel.rate),
                            shippingLabel.refund.refundDate?.formatToMMMddYYYYhhmm() ?: ""
                        )
                    )
                    itemView.shippingLabelItem_trackingNumber.showTrackingLinkButton(false)
                    itemView.shippingLabelList_btnMenu.visibility = View.GONE
                } else {
                    itemView.shippingLabelItem_trackingNumber.setShippingLabelTitle(context.getString(
                        R.string.order_shipment_tracking_number_label
                    ))
                    itemView.shippingLabelItem_trackingNumber.setShippingLabelValue(shippingLabel.trackingNumber)
                    itemView.shippingLabelList_btnMenu.visibility = View.VISIBLE
                    itemView.shippingLabelList_btnMenu.setOnClickListener {
                        showRefundPopup(context, shippingLabel)
                    }

                    shippingLabel.trackingLink?.let {
                        itemView.shippingLabelItem_trackingNumber.showTrackingLinkButton(true)
                        itemView.shippingLabelItem_trackingNumber.setTrackingLinkClickListener {
                            ChromeCustomTabUtils.launchUrl(context, it)
                            AppRatingDialog.incrementInteractions()
                        }
                    } ?: itemView.shippingLabelItem_trackingNumber.showTrackingLinkButton(false)
                }

                itemView.shippingLabelItem_packageInfo.setShippingLabelValue(shippingLabel.packageName)
                itemView.shippingLabelItem_carrierInfo.setShippingLabelValue(
                    context.getString(
                        R.string.orderdetail_shipping_label_carrier_info,
                        shippingLabel.serviceName,
                        formatCurrencyForDisplay(shippingLabel.rate)
                    )
                )

                shippingLabel.originAddress?.let {
                    itemView.shippingLabelItem_shipFrom.setShippingLabelValue(
                        AddressUtils.getFullAddress(
                            it.name, it.getEnvelopeAddress(), it.country.getCountryLabelByCountryCode()
                        )
                    )
                }

                shippingLabel.destinationAddress?.let {
                    itemView.shippingLabelItem_shipTo.setShippingLabelValue(
                        AddressUtils.getFullAddress(
                            it.name, it.getEnvelopeAddress(), it.country.getCountryLabelByCountryCode()
                        )
                    )
                }
            }

            fun bindProductItems(
                context: Context,
                productImageMap: ProductImageMap,
                productItems: List<Order.Item>,
                formatCurrencyForDisplay: (BigDecimal) -> String,
                viewPool: RecyclerView.RecycledViewPool
            ) {
                val childLayoutManager = LinearLayoutManager(context)
                itemView.shippingLabelList_products.apply {
                    layoutManager = childLayoutManager
                    adapter = OrderDetailProductListAdapter(
                        productItems,
                        productImageMap,
                        formatCurrencyForDisplay,
                        false,
                        null
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
                    setRecycledViewPool(viewPool)
                }
            }

            private fun showRefundPopup(
                context: Context,
                item: ShippingLabel
            ) {
                val popup = PopupMenu(context, itemView.shippingLabelList_btnMenu)
                popup.menuInflater.inflate(R.menu.menu_shipping_label, popup.menu)

                popup.menu.findItem(R.id.menu_refund)?.setOnMenuItemClickListener {
                    // TODO: open refund screen
                    true
                }
                popup.show()
            }
        }
    }
}
