package com.woocommerce.android.ui.analytics.daterangeselector

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.AnalyticsDateRangeCardViewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnalyticsDateRangeCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsDateRangeCardViewBinding.inflate(LayoutInflater.from(ctx), this)

    fun setCalendarClickListener(onClickListener: ((view: View) -> Unit)) {
        binding.btnDateRangeSelector.setOnClickListener(onClickListener)
    }

    fun updateFromText(fromDatePeriod: String) {
        binding.tvFromDate.text = fromDatePeriod
    }

    fun updateToText(toDatePeriod: String) {
        binding.tvToDate.text = toDatePeriod
    }
}
