package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.WcEmptyStatsViewBinding
import com.woocommerce.android.util.DateUtils
import java.util.Date

class WCEmptyStatsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = WcEmptyStatsViewBinding.inflate(LayoutInflater.from(ctx), this)

    private val visitorValue
        get() = binding.root.findViewById<MaterialTextView>(R.id.visitors_value)

    private val emptyStatsViewDateTitle
        get() = binding.root.findViewById<MaterialTextView>(R.id.empty_stats_view_date_title)

    private val ordersValue
        get() = binding.root.findViewById<MaterialTextView>(R.id.orders_value)

    private val revenueValue
        get() = binding.root.findViewById<MaterialTextView>(R.id.revenue_value)

    fun updateVisitorCount(visits: Int) {
        visitorValue.text = visits.toString()
        emptyStatsViewDateTitle.text = DateUtils.getDayOfWeekWithMonthAndDayFromDate(Date())

        // The empty view is only shown when there are no orders, which means the revenue is also 0
        ordersValue.text = "0"
        revenueValue.text = "0"
    }
}
