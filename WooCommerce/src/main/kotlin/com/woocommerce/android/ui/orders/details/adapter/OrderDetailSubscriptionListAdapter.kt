package com.woocommerce.android.ui.orders.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailSubscriptionListItemBinding
import com.woocommerce.android.extensions.getMediumDate
import com.woocommerce.android.model.Subscription
import com.woocommerce.android.ui.orders.SubscriptionStatusTag
import com.woocommerce.android.util.CurrencyFormatter

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
                text = subscription.startDate.getMediumDate(context)
            }
            viewBinding.subscriptionStatusTag.tag = SubscriptionStatusTag(subscription.status)
            with(viewBinding.subscriptionTotal) {
                val period = when (subscription.billingPeriod) {
                    Subscription.Period.Day -> context.getString(R.string.subscription_period_day)
                    Subscription.Period.Week -> context.getString(R.string.subscription_period_week)
                    Subscription.Period.Month -> context.getString(R.string.subscription_period_month)
                    Subscription.Period.Year -> context.getString(R.string.subscription_period_year)
                    is Subscription.Period.Custom -> subscription.billingPeriod.value
                }
                text = context.getString(
                    R.string.subscription_total_period,
                    currencyFormatter.formatCurrency(
                        amount = subscription.total,
                        currencyCode = subscription.currency,
                        applyDecimalFormatting = true
                    ),
                    period
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
