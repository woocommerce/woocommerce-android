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

class RefundFeeListAdapter(
    private val checkedChangeListener: OnFeeLineCheckedChangeListener,
    private val formatCurrency: (BigDecimal) -> String
) : RecyclerView.Adapter<RefundFeeListAdapter.ViewHolder>() {
    interface OnFeeLineCheckedChangeListener {
        fun onFeeLineSwitchChanged(isChecked: Boolean, itemId: Long)
    }

    private var items = mutableListOf<FeeRefundListItem>()
    private var selectedItemIds = mutableListOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.refund_fee_list_item, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.price.text = formatCurrency(items[position].feeLine.total)
        holder.name.text = items[position].feeLine.name

        holder.switch.isChecked = selectedItemIds.contains(items[position].feeLine.id)

        // Only show individual toggle if items size is more than 1.
        // Otherwise the main toggle is enough.
        if (items.size > 1) {
            holder.switch.visibility = View.VISIBLE
        }

        holder.switch.setOnCheckedChangeListener { _, isChecked: Boolean ->
            checkedChangeListener.onFeeLineSwitchChanged(isChecked, items[position].feeLine.id)
        }

        if (position == items.size - 1) {
            holder.divider.hide()
        }
    }

    fun update(newItems: List<FeeRefundListItem>) {
        items = newItems.toMutableList()
    }

    fun updateToggleStates(feeLineIds: List<Long>) {
        selectedItemIds = feeLineIds.toMutableList()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val price: TextView = view.findViewById(R.id.issueRefund_feesPrice)
        val name: TextView = view.findViewById(R.id.issueRefund_feesName)
        val switch: SwitchMaterial = view.findViewById(R.id.issueRefund_feeLineSwitch)
        val divider: View = view.findViewById(R.id.issueRefund_feesDivider)
    }

    @Parcelize
    data class FeeRefundListItem(
        val feeLine: Order.FeeLine
    ) : Parcelable {
        fun toDataModel(): WCRefundItem {
            return WCRefundItem(
                feeLine.id,
                quantity = 1, /* Hardcoded because a fee line always has a quantity of 1 */
                subtotal = feeLine.total,
                totalTax = feeLine.totalTax
            )
        }
    }
}
