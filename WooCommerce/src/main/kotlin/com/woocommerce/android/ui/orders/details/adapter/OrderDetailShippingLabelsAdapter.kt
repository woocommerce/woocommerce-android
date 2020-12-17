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
import com.woocommerce.android.databinding.OrderDetailShippingLabelListItemBinding
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToMMMddYYYYhhmm
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.OrderShipmentTrackingHelper
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShippingLabelsAdapter.ShippingLabelsViewHolder
import com.woocommerce.android.widgets.AlignedDividerDecoration
import java.math.BigDecimal

class OrderDetailShippingLabelsAdapter(
    private val formatCurrencyForDisplay: (BigDecimal) -> String,
    private val productImageMap: ProductImageMap,
    private val listener: OnShippingLabelClickListener,
    private val productClickListener: OrderProductActionListener
) : RecyclerView.Adapter<ShippingLabelsViewHolder>() {
    private val viewPool = RecyclerView.RecycledViewPool()

    interface OnShippingLabelClickListener {
        fun onRefundRequested(shippingLabel: ShippingLabel)
        fun onPrintShippingLabelClicked(shippingLabel: ShippingLabel)
    }

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
        val viewBinding = OrderDetailShippingLabelListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ShippingLabelsViewHolder(
            viewBinding,
            viewPool,
            productImageMap,
            formatCurrencyForDisplay,
            listener,
            productClickListener
        )
    }

    override fun onBindViewHolder(holder: ShippingLabelsViewHolder, position: Int) {
        holder.bind(shippingLabels[position])
    }

    override fun getItemCount(): Int = shippingLabels.size

    class ShippingLabelsViewHolder(
        private var viewBinding: OrderDetailShippingLabelListItemBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
        private val productImageMap: ProductImageMap,
        private val formatCurrencyForDisplay: (BigDecimal) -> String,
        private val listener: OnShippingLabelClickListener,
        private val productClickListener: OrderProductActionListener
    ) : RecyclerView.ViewHolder(
        viewBinding.root
    ) {
        fun bind(shippingLabel: ShippingLabel) {
            // display product list if product list is not empty
            if (shippingLabel.products.isNotEmpty()) {
                with(viewBinding.shippingLabelListProducts) {
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
            }

            if (shippingLabel.id == 0L) {
                viewBinding.shippingLabelItemTrackingNumber.isVisible = false
                viewBinding.shippingLabelItemMorePanel.isVisible = false
                with(viewBinding.shippingLabelListLblPackage) {
                    text = context.getString(R.string.orderdetail_shipping_label_unpackaged_products_header)
                }
                return
            }

            // display tracking number details if shipping label is not refunded
            val isRefunded = shippingLabel.refund == null
            viewBinding.shippingLabelListBtnMenu.isVisible = isRefunded
            viewBinding.shippingLabelListPrintBtn.isVisible = isRefunded
            viewBinding.shippingLabelListProducts.isVisible = isRefunded
            with(viewBinding.shippingLabelItemTrackingNumber) {
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
                    viewBinding.shippingLabelListBtnMenu.setOnClickListener {
                        showRefundPopup(shippingLabel, listener)
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

                    viewBinding.shippingLabelListPrintBtn.setOnClickListener {
                        listener.onPrintShippingLabelClicked(shippingLabel)
                    }
                }
            }

            // click on view more details section
            viewBinding.shippingLabelItemViewMore.setOnClickListener {
                val isChecked = viewBinding.shippingLabelItemViewMoreButtonImage.rotation == 0F
                if (isChecked) {
                    viewBinding.shippingLabelItemMorePanel.expand()
                    viewBinding.shippingLabelItemViewMoreButtonImage.animate().rotation(180F).setDuration(200).start()
                    with(viewBinding.shippingLabelItemViewMoreButtonTitle) {
                        text = context.getString(R.string.orderdetail_shipping_label_item_hide_shipping)
                    }
                } else {
                    viewBinding.shippingLabelItemMorePanel.collapse()
                    viewBinding.shippingLabelItemViewMoreButtonImage.animate().rotation(0F).setDuration(200).start()
                    with(viewBinding.shippingLabelItemViewMoreButtonTitle) {
                        text = context.getString(R.string.orderdetail_shipping_label_item_show_shipping)
                    }
                }
            }

            // Shipping label header
            with(viewBinding.shippingLabelListLblPackage) {
                text = context.getString(
                    R.string.orderdetail_shipping_label_item_header,
                    adapterPosition + 1
                )
            }

            // display origin address
            shippingLabel.originAddress?.let {
                viewBinding.shippingLabelItemShipFrom.setShippingLabelValue(
                    it.getFullAddress(
                        it.firstName, it.getEnvelopeAddress(), it.getCountryLabelByCountryCode()
                    )
                )
            }

            // display destination address
            shippingLabel.destinationAddress?.let {
                viewBinding.shippingLabelItemShipTo.setShippingLabelValue(
                    it.getFullAddress(
                        it.firstName, it.getEnvelopeAddress(), it.getCountryLabelByCountryCode()
                    )
                )
            }

            // Shipping label package info
            with(viewBinding.shippingLabelItemPackageInfo) { setShippingLabelValue(shippingLabel.packageName) }

            // Shipping label carrier info
            with(viewBinding.shippingLabelItemCarrierInfo) {
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
            listener: OnShippingLabelClickListener
        ) {
            val popup = PopupMenu(itemView.context, viewBinding.shippingLabelListBtnMenu)
            popup.menuInflater.inflate(R.menu.menu_shipping_label, popup.menu)

            popup.menu.findItem(R.id.menu_refund)?.setOnMenuItemClickListener {
                listener.onRefundRequested(shippingLabel)
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
