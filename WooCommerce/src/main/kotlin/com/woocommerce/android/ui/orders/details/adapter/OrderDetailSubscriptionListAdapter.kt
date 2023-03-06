package com.woocommerce.android.ui.orders.details.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailSubscriptionListItemBinding
import com.woocommerce.android.extensions.getMediumDate
import com.woocommerce.android.model.Subscription
import com.woocommerce.android.model.Subscription.Period
import com.woocommerce.android.ui.orders.SubscriptionStatusTag
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.StringUtils

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
            subscription.endDate?.let { endDate ->
                viewBinding.subscriptionEndDate.visibility = View.VISIBLE
                with(viewBinding.subscriptionEndDate) {
                    text = context.getString(R.string.subscription_end_date, endDate.getMediumDate(context))
                }
            } ?: run {
                viewBinding.subscriptionEndDate.visibility = View.GONE
            }

            viewBinding.subscriptionStatusTag.tag = SubscriptionStatusTag(subscription.status)
            with(viewBinding.subscriptionTotal) {
                text = context.getString(
                    R.string.subscription_total,
                    currencyFormatter.formatCurrency(
                        amount = subscription.total,
                        currencyCode = subscription.currency,
                        applyDecimalFormatting = true
                    )
                )
            }
            with(viewBinding.subscriptionPeriod) {
                val period = getBillingPeriod(context, subscription.billingPeriod, subscription.billingInterval)
                text = if (subscription.billingInterval > 1) {
                    context.getString(
                        R.string.subscription_period_interval_multiple,
                        subscription.billingInterval,
                        period
                    )
                } else {
                    context.getString(R.string.subscription_period_interval_single, period)
                }
            }
        }

        private fun getBillingPeriod(
            context: Context,
            billingPeriod: Period,
            billingInterval: Int
        ): String {
            return when (billingPeriod) {
                Period.Day -> StringUtils.getQuantityString(
                    context = context,
                    quantity = billingInterval,
                    default = R.string.subscription_period_multiple_days,
                    one = R.string.subscription_period_day
                )
                Period.Week -> StringUtils.getQuantityString(
                    context = context,
                    quantity = billingInterval,
                    default = R.string.subscription_period_multiple_weeks,
                    one = R.string.subscription_period_week
                )
                Period.Month -> StringUtils.getQuantityString(
                    context = context,
                    quantity = billingInterval,
                    default = R.string.subscription_period_multiple_months,
                    one = R.string.subscription_period_month
                )
                Period.Year -> StringUtils.getQuantityString(
                    context = context,
                    quantity = billingInterval,
                    default = R.string.subscription_period_multiple_years,
                    one = R.string.subscription_period_year
                )
                is Period.Custom -> billingPeriod.value
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
