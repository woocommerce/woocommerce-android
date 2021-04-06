package com.woocommerce.android.ui.refunds

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.woocommerce.android.R
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.model.Order
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import java.math.BigDecimal

class RefundShippingListAdapter(
    private val checkedChangeListener: OnCheckedChangeListener,
    private val formatCurrency: (BigDecimal) -> String
) : RecyclerView.Adapter<RefundShippingListAdapter.ViewHolder>() {
    interface OnCheckedChangeListener {
        fun onShippingLineSwitchChanged(isChecked: Boolean, itemId: Long)
    }

    private var items = mutableListOf<ShippingRefundListItem>()
    private var selectedItemIds = mutableListOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.refund_shipping_list_item, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.price.text = formatCurrency(items[position].shippingLine.total)
        holder.name.text = items[position].shippingLine.methodTitle

        holder.switch.isChecked = selectedItemIds.contains(items[position].shippingLine.itemId)

        // Only show individual toggle if items size is more than 1.
        // Otherwise the main toggle is enough.
        if (items.size > 1) {
            holder.switch.visibility = View.VISIBLE
        }

        holder.switch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            checkedChangeListener.onShippingLineSwitchChanged(isChecked, items[position].shippingLine.itemId)
        }

        if (position == items.size - 1) {
            holder.divider.hide()
        }
    }

    fun update(newItems: List<ShippingRefundListItem>) {
        items = newItems.toMutableList()
    }

    fun updateToggleStates(shippingLineIds: List<Long>) {
        selectedItemIds = shippingLineIds.toMutableList()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val price: TextView = view.findViewById(R.id.issueRefund_shippingPrice)
        val name: TextView = view.findViewById(R.id.issueRefund_shippingName)
        val switch: SwitchMaterial = view.findViewById(R.id.issueRefund_shippingLineSwitch)
        val divider: View = view.findViewById(R.id.issueRefund_shippingDivider)
    }

    @Parcelize
    data class ShippingRefundListItem(
        val shippingLine: Order.ShippingLine
    ) : Parcelable {
        fun toDataModel(): WCRefundItem {
            return WCRefundItem(
                shippingLine.itemId,
                quantity = 1, /* Hardcoded because a shipping line always has a quantity of 1 */
                subtotal = shippingLine.total,
                totalTax = shippingLine.totalTax
            )
        }
    }
}
