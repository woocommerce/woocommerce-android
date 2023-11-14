package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailSubscriptionListBinding
import com.woocommerce.android.model.Subscription
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailSubscriptionListAdapter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.widgets.AlignedDividerDecoration

class OrderDetailSubscriptionListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailSubscriptionListBinding.inflate(LayoutInflater.from(ctx), this)
    fun updateSubscriptionList(subscriptions: List<Subscription>, currencyFormatter: CurrencyFormatter) {
        val subscriptionAdapter =
            binding.subscriptionItems.adapter as? OrderDetailSubscriptionListAdapter
                ?: OrderDetailSubscriptionListAdapter(currencyFormatter).also {
                    binding.subscriptionItems.apply {
                        setHasFixedSize(true)
                        layoutManager = LinearLayoutManager(context)
                        itemAnimator = DefaultItemAnimator()
                        adapter = it
                        if (itemDecorationCount == 0) {
                            addItemDecoration(
                                AlignedDividerDecoration(
                                    context,
                                    DividerItemDecoration.VERTICAL,
                                    R.id.subscription_id
                                )
                            )
                        }
                    }
                }
        subscriptionAdapter.subscriptionList = subscriptions
    }
}
