package com.woocommerce.android.ui.orders.details.adapter

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailRefundPaymentItemBinding
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.Refund
import java.math.BigDecimal

class OrderDetailRefundsAdapter(
    private val isCashPayment: Boolean,
    private val paymentMethodTitle: String,
    private val orderDetailRefundsLineBuilder: OrderDetailRefundsLineBuilder,
    private val formatCurrency: (BigDecimal) -> String
) : RecyclerView.Adapter<OrderDetailRefundsAdapter.ViewHolder>() {
    var refundList: List<Refund> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                RefundModelDiffCallback(
                    field,
                    value
                ),
                true
            )
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): ViewHolder {
        return ViewHolder(
            OrderDetailRefundPaymentItemBinding.inflate(
                LayoutInflater.from(parent.context)
            ),
            isCashPayment,
            paymentMethodTitle,
            orderDetailRefundsLineBuilder,
            formatCurrency
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(refundList[position])
    }

    override fun getItemCount(): Int = refundList.size

    class ViewHolder(
        private val viewBinding: OrderDetailRefundPaymentItemBinding,
        private val isCashPayment: Boolean,
        private val paymentMethodTitle: String,
        private val orderDetailRefundsLineBuilder: OrderDetailRefundsLineBuilder,
        private val formatCurrency: (BigDecimal) -> String
    ) : RecyclerView.ViewHolder(
        viewBinding.root
    ) {
        fun bind(refund: Refund) {
            val context = viewBinding.root.context

            val refundLine = orderDetailRefundsLineBuilder.buildRefundLine(refund)
            viewBinding.refundsListLblRefund.text = context.getString(
                R.string.orderdetail_refunded_line_with_info,
                refundLine
            )
            viewBinding.refundsListRefundAmount.text = context.getString(
                R.string.orderdetail_refund_amount,
                formatCurrency(refund.amount)
            )
            viewBinding.refundsListRefundMethod.text =
                itemView.resources.getString(R.string.orderdetail_refund_detail).format(
                    DateFormat.getMediumDateFormat(context).format(refund.dateCreated),
                    refund.getRefundMethod(
                        paymentMethodTitle = paymentMethodTitle,
                        isCashPayment = isCashPayment,
                        defaultValue = itemView.context.getString(R.string.order_refunds_manual_refund)
                    )
                )

            viewBinding.refundsListItemRoot.setOnClickListener {
                // TODO: open refund detail screen
            }
        }
    }

    class RefundModelDiffCallback(
        private val oldList: List<Refund>,
        private val newList: List<Refund>
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
