package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShipmentTrackingListAdapter
import kotlinx.android.synthetic.main.order_detail_shipment_tracking_list.view.*

class OrderDetailShipmentTrackingListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_shipment_tracking_list, this)
    }

    fun updateShipmentTrackingList(
        shipmentTrackings: List<OrderShipmentTracking>,
        onDeleteShipmentTrackingClicked: (trackingNumber: String) -> Unit
    ) {
        val shipmentTrackingAdapter =
            shipmentTrack_items.adapter as? OrderDetailShipmentTrackingListAdapter
                ?: OrderDetailShipmentTrackingListAdapter(onDeleteShipmentTrackingClicked)

        shipmentTrack_items.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            adapter = shipmentTrackingAdapter
        }
        shipmentTrackingAdapter.shipmentTrackingList = shipmentTrackings
    }

    fun showAddTrackingButton(
        show: Boolean,
        onTap: () -> Unit
    ) {
        shipmentTrack_btnAddTracking.isVisible = show
        shipmentTrack_btnAddTracking.setOnClickListener { onTap() }
    }
}
