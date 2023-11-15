package com.woocommerce.android.ui.orders.details.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailSubscriptionListItemBinding
import com.woocommerce.android.model.Subscription
import com.woocommerce.android.ui.orders.SubscriptionStatusTag
import com.woocommerce.android.util.CurrencyFormatter
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class OrderDetailSubscriptionListAdapter(private val currencyFormatter: CurrencyFormatter) :
    RecyclerView.Adapter<OrderDetailSubscriptionListAdapter.OrderDetailSubscriptionViewHolder>() {
    var subscriptionList: List<Subscription> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                SubscriptionDiffCallback(
                    field,
                    value
                ),
                true
            )
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderDetailSubscriptionViewHolder {
        val viewBinding = OrderDetailSubscriptionListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderDetailSubscriptionViewHolder(viewBinding, currencyFormatter)
    }

    override fun onBindViewHolder(holder: OrderDetailSubscriptionViewHolder, position: Int) {
        holder.bind(subscriptionList[position])
    }

    override fun getItemCount(): Int = subscriptionList.size

    class OrderDetailSubscriptionViewHolder(
        private val viewBinding: OrderDetailSubscriptionListItemBinding,
        private val currencyFormatter: CurrencyFormatter
    ) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(subscription: Subscription) {
            with(viewBinding.subscriptionId) {
                text = context.getString(R.string.subscription_id, subscription.id)
            }
            with(viewBinding.subscriptionStartDate) {
                text = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(subscription.startDate)
            }
            subscription.endDate?.let { endDate ->
                viewBinding.subscriptionEndDate.visibility = View.VISIBLE
                with(viewBinding.subscriptionEndDate) {
                    val endDateString =
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(endDate)
                    text = context.getString(R.string.subscription_end_date, endDateString)
                }
            } ?: run {
                viewBinding.subscriptionEndDate.visibility = View.GONE
            }

            viewBinding.subscriptionStatusTag.tag = SubscriptionStatusTag(subscription.status)
            with(viewBinding.subscriptionTotal) {
                val periodDescription = subscription.billingPeriod.formatWithInterval(
                    context = context,
                    interval = subscription.billingInterval
                )

                text = context.getString(
                    R.string.subscription_total,
                    currencyFormatter.formatCurrency(
                        amount = subscription.total,
                        currencyCode = subscription.currency,
                        applyDecimalFormatting = true
                    ),
                    periodDescription
                )
            }
        }
    }

    class SubscriptionDiffCallback(
        private val oldList: List<Subscription>,
        private val newList: List<Subscription>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
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
