package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.order_detail_shipment_tracking_list.view.*
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

class OrderDetailShipmentTrackingListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_shipment_tracking_list, this)
    }

    fun initView(trackings: List<WCOrderShipmentTrackingModel>) {
        val viewManager = LinearLayoutManager(context)
        val viewAdapter = ShipmentTrackingListAdapter(trackings)

        shipmentTrack_items.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = viewAdapter
        }
    }

    class ShipmentTrackingListAdapter(
        private val trackings: List<WCOrderShipmentTrackingModel>
    ) : RecyclerView.Adapter<ShipmentTrackingListAdapter.ViewHolder>() {
        class ViewHolder(val view: OrderDetailShipmentTrackingItemView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: OrderDetailShipmentTrackingItemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.order_detail_shipment_tracking_list_item, parent, false)
                    as OrderDetailShipmentTrackingItemView
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.view.initView(trackings[position])
        }

        override fun getItemCount() = trackings.size
    }
}
