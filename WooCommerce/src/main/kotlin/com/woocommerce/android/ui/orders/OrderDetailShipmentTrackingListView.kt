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
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_FULFILLMENT_TRACKING_ADD_TRACKING_BUTTON_TAPPED
import com.woocommerce.android.ui.base.UIMessageResolver
import kotlinx.android.synthetic.main.order_detail_shipment_tracking_list.view.*
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

class OrderDetailShipmentTrackingListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_shipment_tracking_list, this)
    }

    // negative IDs denote transient tracking providers
    private var nextTransientTrackingId = -1

    fun initView(
        trackings: List<WCOrderShipmentTrackingModel>,
        uiMessageResolver: UIMessageResolver,
        allowAddTrackingOption: Boolean = false,
        shipmentTrackingActionListener: OrderShipmentTrackingActionListener? = null
    ) {
        val viewManager = LinearLayoutManager(context)
        val viewAdapter = ShipmentTrackingListAdapter(
                trackings.toMutableList(),
                uiMessageResolver,
                allowAddTrackingOption
        )

        shipmentTrack_items.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = viewAdapter
        }

        if (allowAddTrackingOption) {
            shipmentTrack_label.text = context.getString(R.string.order_shipment_tracking_add_label)
            shipmentTrack_btnAddTracking.visibility = View.VISIBLE
            shipmentTrack_btnAddTracking.setOnClickListener {
                AnalyticsTracker.track(ORDER_FULFILLMENT_TRACKING_ADD_TRACKING_BUTTON_TAPPED)
                shipmentTrackingActionListener?.openAddOrderShipmentTrackingScreen()
            }
        }
    }

    /*
     * a transient note is a temporary placeholder created after the user adds a provider but before the request to
     * add the provider has completed - this enables us to be optimistic about connectivity
     */
    fun addTransientTrackingProvider(
        provider: String,
        trackingNum: String,
        dateShipped: String
    ) {
        enableItemAnimator(true)
        val wcOrderShipmentTrackingModel = WCOrderShipmentTrackingModel(nextTransientTrackingId)
        wcOrderShipmentTrackingModel.trackingNumber = trackingNum
        wcOrderShipmentTrackingModel.dateShipped = dateShipped
        wcOrderShipmentTrackingModel.trackingProvider = provider
        (shipmentTrack_items.adapter as ShipmentTrackingListAdapter).addNote(wcOrderShipmentTrackingModel)
        nextTransientTrackingId--
        shipmentTrack_items.scrollToPosition(0)
    }

    private fun enableItemAnimator(enable: Boolean) {
        shipmentTrack_items.itemAnimator = if (enable) DefaultItemAnimator() else null
    }

    class ShipmentTrackingListAdapter(
        private val trackings: MutableList<WCOrderShipmentTrackingModel>,
        private val uiMessageResolver: UIMessageResolver,
        private val allowAddTrackingOption: Boolean
    ) : RecyclerView.Adapter<ShipmentTrackingListAdapter.ViewHolder>() {
        class ViewHolder(val view: OrderDetailShipmentTrackingItemView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: OrderDetailShipmentTrackingItemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.order_detail_shipment_tracking_list_item, parent, false)
                    as OrderDetailShipmentTrackingItemView
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.view.initView(trackings[position], uiMessageResolver, allowAddTrackingOption)
        }

        override fun getItemCount() = trackings.size

        fun addNote(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel) {
            trackings.add(0, wcOrderShipmentTrackingModel)
            notifyItemInserted(0)
        }
    }
}
