package com.woocommerce.android.ui.orders.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToMMMddYYYYhhmm
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.OrderShipmentTrackingHelper
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShippingLabelsAdapter.ShippingLabelsViewHolder
import com.woocommerce.android.widgets.AlignedDividerDecoration
import kotlinx.android.synthetic.main.order_detail_shipping_label_list_item.view.*
import java.math.BigDecimal

class OrderDetailShippingLabelsAdapter(
    private val formatCurrencyForDisplay: (BigDecimal) -> String,
    private val productImageMap: ProductImageMap,
    private val onRefundRequested: (shippingLabel: ShippingLabel) -> Unit,
    private val productClickListener: OrderProductActionListener
) : RecyclerView.Adapter<ShippingLabelsViewHolder>() {
    private val viewPool = RecyclerView.RecycledViewPool()

    var shippingLabels: List<ShippingLabel> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                ShippingLabelDiffCallback(
                    field,
                    value
                ), true)
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): ShippingLabelsViewHolder {
        return ShippingLabelsViewHolder(
            parent, viewPool, productImageMap, formatCurrencyForDisplay, onRefundRequested, productClickListener
        )
    }

    override fun onBindViewHolder(holder: ShippingLabelsViewHolder, position: Int) {
        holder.bind(shippingLabels[position])
    }

    override fun getItemCount(): Int = shippingLabels.size

    class ShippingLabelsViewHolder(
        parent: ViewGroup,
        private val viewPool: RecyclerView.RecycledViewPool,
        private val productImageMap: ProductImageMap,
        private val formatCurrencyForDisplay: (BigDecimal) -> String,
        private val onRefundRequested: (shippingLabel: ShippingLabel) -> Unit,
        private val productClickListener: OrderProductActionListener
    ) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.order_detail_shipping_label_list_item, parent, false)
    ) {
        fun bind(shippingLabel: ShippingLabel) {
            // display product list
            with(itemView.shippingLabelList_products) {
                layoutManager = LinearLayoutManager(context)
                adapter = OrderDetailProductListAdapter(
                    shippingLabel.products,
                    productImageMap,
                    formatCurrencyForDisplay,
                    productClickListener
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

            if (shippingLabel.id == 0L) {
                itemView.shippingLabelItem_trackingNumber.isVisible = false
                itemView.shippingLabelItem_morePanel.isVisible = false
                with(itemView.shippingLabelList_lblPackage) {
                    text = context.getString(R.string.orderdetail_shipping_label_unpackaged_products_header)
                }
                return
            }

            // display tracking number details if shipping label is not refunded
            itemView.shippingLabelList_btnMenu.isVisible = shippingLabel.refund == null
            with(itemView.shippingLabelItem_trackingNumber) {
                if (shippingLabel.refund != null) {
                    setShippingLabelTitle(
                        context.getString(
                            R.string.orderdetail_shipping_label_refund_title,
                            shippingLabel.serviceName
                        ))
                    setShippingLabelValue(
                        context.getString(R.string.orderdetail_shipping_label_refund_subtitle,
                            formatCurrencyForDisplay(shippingLabel.rate),
                            shippingLabel.refund.refundDate?.formatToMMMddYYYYhhmm() ?: ""
                        ))
                    showTrackingItemButton(false)
                } else {
                    setShippingLabelTitle(context.getString(
                        R.string.order_shipment_tracking_number_label
                    ))
                    setShippingLabelValue(shippingLabel.trackingNumber)
                    itemView.shippingLabelList_btnMenu.setOnClickListener {
                        showRefundPopup(shippingLabel, onRefundRequested)
                    }

                    // display tracking link if available
                    if (shippingLabel.trackingNumber.isNotEmpty()) {
                        showTrackingItemButton(true)
                        setTrackingItemClickListener {
                            OrderShipmentTrackingHelper.showTrackingOrDeleteOptionPopup(
                                getTrackingItemButton(), context, shippingLabel.trackingLink,
                                shippingLabel.trackingNumber
                            )
                        }
                    } else showTrackingItemButton(false)
                }
            }

            // click on view more details section
            itemView.shippingLabelItem_viewMore.setOnClickListener {
                val isChecked = itemView.shippingLabelItem_viewMoreButtonImage.rotation == 0F
                if (isChecked) {
                    itemView.shippingLabelItem_morePanel.expand()
                    itemView.shippingLabelItem_viewMoreButtonImage.animate().rotation(180F).setDuration(200).start()
                    with(itemView.shippingLabelItem_viewMoreButtonTitle) {
                        text = context.getString(R.string.orderdetail_shipping_label_item_hide_shipping)
                    }
                } else {
                    itemView.shippingLabelItem_morePanel.collapse()
                    itemView.shippingLabelItem_viewMoreButtonImage.animate().rotation(0F).setDuration(200).start()
                    with(itemView.shippingLabelItem_viewMoreButtonTitle) {
                        text = context.getString(R.string.orderdetail_shipping_label_item_show_shipping)
                    }
                }
            }

            // Shipping label header
            with(itemView.shippingLabelList_lblPackage) {
                text = context.getString(
                    R.string.orderdetail_shipping_label_item_header,
                    adapterPosition + 1
                )
            }

            // display origin address
            shippingLabel.originAddress?.let {
                itemView.shippingLabelItem_shipFrom.setShippingLabelValue(
                    it.getFullAddress(
                        it.firstName, it.getEnvelopeAddress(), it.getCountryLabelByCountryCode()
                    )
                )
            }

            // display destination address
            shippingLabel.destinationAddress?.let {
                itemView.shippingLabelItem_shipTo.setShippingLabelValue(
                    it.getFullAddress(
                        it.firstName, it.getEnvelopeAddress(), it.getCountryLabelByCountryCode()
                    )
                )
            }

            // Shipping label package info
            with(itemView.shippingLabelItem_packageInfo) { setShippingLabelValue(shippingLabel.packageName) }

            // Shipping label carrier info
            with(itemView.shippingLabelItem_carrierInfo) {
                setShippingLabelValue(
                    context.getString(
                        R.string.orderdetail_shipping_label_carrier_info,
                        shippingLabel.serviceName,
                        formatCurrencyForDisplay(shippingLabel.rate)
                    )
                )
            }
        }

        private fun showRefundPopup(
            shippingLabel: ShippingLabel,
            onItemSelected: (shippingLabel: ShippingLabel) -> Unit
        ) {
            val popup = PopupMenu(itemView.context, itemView.shippingLabelList_btnMenu)
            popup.menuInflater.inflate(R.menu.menu_shipping_label, popup.menu)

            popup.menu.findItem(R.id.menu_refund)?.setOnMenuItemClickListener {
                onItemSelected(shippingLabel)
                true
            }
            popup.show()
        }
    }

    class ShippingLabelDiffCallback(
        private val oldList: List<ShippingLabel>,
        private val newList: List<ShippingLabel>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old == new
        }
    }
}
