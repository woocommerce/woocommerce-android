package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.OrderDetailShipmentTrackingListBinding
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShipmentTrackingListAdapter

class OrderDetailShipmentTrackingListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailShipmentTrackingListBinding.inflate(LayoutInflater.from(ctx), this)

    fun updateShipmentTrackingList(
        shipmentTrackings: List<OrderShipmentTracking>,
        onDeleteShipmentTrackingClicked: (trackingNumber: String) -> Unit
    ) {
        val shipmentTrackingAdapter =
            binding.shipmentTrackItems.adapter as? OrderDetailShipmentTrackingListAdapter
                ?: OrderDetailShipmentTrackingListAdapter(onDeleteShipmentTrackingClicked)

        binding.shipmentTrackItems.apply {
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
        binding.shipmentTrackBtnAddTracking.isVisible = show
        binding.shipmentTrackBtnAddTracking.setOnClickListener { onTap() }
    }
}
