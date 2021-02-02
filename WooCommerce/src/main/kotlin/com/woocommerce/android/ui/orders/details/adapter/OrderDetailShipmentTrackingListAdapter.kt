package com.woocommerce.android.ui.orders.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.OrderDetailShipmentTrackingListItemBinding
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.ui.orders.OrderShipmentTrackingHelper
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShipmentTrackingListAdapter.OrderDetailShipmentTrackingViewHolder
import com.woocommerce.android.util.DateUtils

class OrderDetailShipmentTrackingListAdapter(
    private val onDeleteShipmentTrackingClicked: (trackingNumber: String) -> Unit
) : RecyclerView.Adapter<OrderDetailShipmentTrackingViewHolder>() {
    var shipmentTrackingList: List<OrderShipmentTracking> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                ShipmentTrackingDiffCallback(
                    field,
                    value
                ), true)
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): OrderDetailShipmentTrackingViewHolder {
        val viewBinding = OrderDetailShipmentTrackingListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderDetailShipmentTrackingViewHolder(viewBinding, onDeleteShipmentTrackingClicked)
    }

    override fun onBindViewHolder(holder: OrderDetailShipmentTrackingViewHolder, position: Int) {
        holder.bind(shipmentTrackingList[position])
    }

    override fun getItemCount(): Int = shipmentTrackingList.size

    class OrderDetailShipmentTrackingViewHolder(
        private val viewBinding: OrderDetailShipmentTrackingListItemBinding,
        private val onDeleteShipmentTrackingClicked: (trackingNumber: String) -> Unit
    ) : RecyclerView.ViewHolder(
        viewBinding.root
    ) {
        fun bind(shipmentTracking: OrderShipmentTracking) {
            with(viewBinding.trackingType) { text = shipmentTracking.trackingProvider }
            with(viewBinding.trackingNumber) { text = shipmentTracking.trackingNumber }
            with(viewBinding.trackingDateShipped) {
                text = DateUtils().getLocalizedLongDateString(context, shipmentTracking.dateShipped).orEmpty()
            }
            with(viewBinding.trackingBtnTrack) {
                isVisible = true
                setOnClickListener {
                    OrderShipmentTrackingHelper.showTrackingOrDeleteOptionPopup(
                        anchor = it,
                        context = context,
                        trackingLink = shipmentTracking.trackingLink,
                        trackingNumber = shipmentTracking.trackingNumber,
                        onDeleteTrackingClicked = onDeleteShipmentTrackingClicked
                    )
                }
            }
        }
    }

    class ShipmentTrackingDiffCallback(
        private val oldList: List<OrderShipmentTracking>,
        private val newList: List<OrderShipmentTracking>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].remoteTrackingId == newList[newItemPosition].remoteTrackingId
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
