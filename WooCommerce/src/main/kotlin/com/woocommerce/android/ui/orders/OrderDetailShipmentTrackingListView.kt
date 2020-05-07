package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.UIMessageResolver
import kotlinx.android.synthetic.main.order_detail_shipment_tracking_list.view.*
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

class OrderDetailShipmentTrackingListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_shipment_tracking_list, this)
    }

    // negative IDs denote transient tracking providers
    private var nextTransientTrackingId = -1

    /**
     * @param [isOrderDetail] is true, then
     * 1. the delete icon would not be displayed. The hanburger menu should be displayed instead
     * 2. The title should be "Tracking" instead of "Optional shipment tracking"
     */
    fun initView(
        trackings: List<WCOrderShipmentTrackingModel>,
        uiMessageResolver: UIMessageResolver,
        isOrderDetail: Boolean,
        shipmentTrackingActionListener: OrderShipmentTrackingActionListener? = null
    ) {
        val viewManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        val viewAdapter = ShipmentTrackingListAdapter(
                trackings.toMutableList(),
                uiMessageResolver,
                isOrderDetail,
                shipmentTrackingActionListener
        )

        shipmentTrack_items.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            adapter = viewAdapter
        }

        showOrHideDivider()
        shipmentTrack_btnAddTracking.visibility = View.VISIBLE
        shipmentTrack_btnAddTracking.setOnClickListener {
            shipmentTrackingActionListener?.openAddOrderShipmentTrackingScreen()
        }

        if (!isOrderDetail) {
            shipmentTrack_label.text = context.getString(R.string.order_shipment_tracking_add_label)
        }
    }

    /**
     * Divider should only be displayed when there is at least one tracking
     * item in the list
     */
    private fun showOrHideDivider() {
        val show = getShipmentTrackingCount()?.let { it > 0 } ?: false
        if (show) {
            shipmentTrack_divider.visibility = View.VISIBLE
        } else {
            shipmentTrack_divider.visibility = View.GONE
        }
    }

    /**
     * a transient note is a temporary placeholder created after the user adds a provider but before the request to
     * add the provider has completed - this enables us to be optimistic about connectivity
     */
    fun addTransientTrackingProvider(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel) {
        wcOrderShipmentTrackingModel.id = nextTransientTrackingId
        (shipmentTrack_items.adapter as? ShipmentTrackingListAdapter)?.addTracking(wcOrderShipmentTrackingModel)
        nextTransientTrackingId--
        shipmentTrack_items.scrollToPosition(0)
        showOrHideDivider()
    }

    fun deleteTrackingProvider(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel) {
        (shipmentTrack_items.adapter as ShipmentTrackingListAdapter).deleteTracking(wcOrderShipmentTrackingModel)
        shipmentTrack_items.swapAdapter(shipmentTrack_items.adapter, false)
        showOrHideDivider()
    }

    fun undoDeleteTrackingRecord(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel) {
        (shipmentTrack_items.adapter as ShipmentTrackingListAdapter).undoDeleteTracking(wcOrderShipmentTrackingModel)
        shipmentTrack_items.swapAdapter(shipmentTrack_items.adapter, false)
        showOrHideDivider()
    }

    fun getShipmentTrackingCount() = shipmentTrack_items.adapter?.itemCount

    class ShipmentTrackingListAdapter(
        private val trackings: MutableList<WCOrderShipmentTrackingModel>,
        private val uiMessageResolver: UIMessageResolver,
        private val isOrderDetail: Boolean,
        private val shipmentTrackingActionListener: OrderShipmentTrackingActionListener?
    ) : RecyclerView.Adapter<ShipmentTrackingListAdapter.ViewHolder>() {
        private var deletedTrackingModelIndex = -1

        class ViewHolder(val view: OrderDetailShipmentTrackingItemView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: OrderDetailShipmentTrackingItemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.order_detail_shipment_tracking_list_item, parent, false)
                    as OrderDetailShipmentTrackingItemView
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.view.initView(
                    item = trackings[position],
                    uiMessageResolver = uiMessageResolver,
                    isOrderDetail = isOrderDetail,
                    shipmentTrackingActionListener = shipmentTrackingActionListener
            )
        }

        override fun getItemCount() = trackings.size

        fun addTracking(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel) {
            trackings.add(0, wcOrderShipmentTrackingModel)
            notifyItemInserted(0)
        }

        fun deleteTracking(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel) {
            deletedTrackingModelIndex = trackings.indexOf(wcOrderShipmentTrackingModel)
            trackings.remove(wcOrderShipmentTrackingModel)
            notifyItemRemoved(deletedTrackingModelIndex)
        }

        fun undoDeleteTracking(wcOrderShipmentTrackingModel: WCOrderShipmentTrackingModel) {
            trackings.add(deletedTrackingModelIndex, wcOrderShipmentTrackingModel)
            notifyItemInserted(deletedTrackingModelIndex)
            deletedTrackingModelIndex = -1
        }
    }
}
