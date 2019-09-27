package com.woocommerce.android.ui.orders

import android.annotation.SuppressLint
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isEqualTo
import org.wordpress.android.fluxc.model.refunds.RefundModel
import java.math.BigDecimal

class OrderDetailRefundListAdapter(
    private val formatCurrency: (BigDecimal) -> String
) : RecyclerView.Adapter<OrderDetailRefundListAdapter.ViewHolder>() {
    private var items = ArrayList<RefundModel>()

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): ViewHolder {
        return ViewHolder(parent, formatCurrency)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<RefundModel>) {
        val diffResult = DiffUtil.calculateDiff(RefundModelDiffCallback(items, newItems))
        items = ArrayList(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    class ViewHolder(parent: ViewGroup, private val formatCurrency: (BigDecimal) -> String) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.order_detail_refund_payment_item, parent, false)
    ) {
        private val amount: TextView = itemView.findViewById(R.id.refundsList_refundAmount)
        private val method: TextView = itemView.findViewById(R.id.refundsList_refundMethod)

        @SuppressLint("SetTextI18n") fun bind(refund: RefundModel) {
            amount.text = "-${formatCurrency(refund.amount)}"
            method.text = itemView.resources.getString(R.string.orderdetail_refund_detail).format(
                    DateFormat.getMediumDateFormat(itemView.context).format(refund.dateCreated),
                    itemView.resources.getString(R.string.order_refunds_manual_refund), // TODO: Change after auto refunds implemented
                    itemView.resources.getString(R.string.orderdetail_refund_view_details)
            )
        }
    }

    class RefundModelDiffCallback(
        private val oldList: List<RefundModel>,
        private val newList: List<RefundModel>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.amount isEqualTo new.amount &&
                    old.dateCreated == new.dateCreated &&
                    old.reason == new.reason &&
                    old.items == new.items
        }
    }
}
