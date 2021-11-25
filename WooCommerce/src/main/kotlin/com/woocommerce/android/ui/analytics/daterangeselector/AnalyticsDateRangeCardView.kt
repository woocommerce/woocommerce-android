package com.woocommerce.android.ui.analytics.daterangeselector

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.AnalyticsDateRangeCardViewBinding
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorContract.DateRangeEvent
import dagger.hilt.android.AndroidEntryPoint

class AnalyticsDateRangeCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsDateRangeCardViewBinding.inflate(LayoutInflater.from(ctx), this)

    internal fun initView(dateRangeEvent: DateRangeEvent) {
        binding.btnDateRangeSelector.setOnClickListener { dateRangeEvent.onDateRangeCalendarClickEvent() }
    }
}
