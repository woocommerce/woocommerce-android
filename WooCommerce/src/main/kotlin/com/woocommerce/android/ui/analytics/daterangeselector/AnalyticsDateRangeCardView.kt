package com.woocommerce.android.ui.analytics.daterangeselector

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.databinding.AnalyticsDateRangeCardViewBinding

class AnalyticsDateRangeCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsDateRangeCardViewBinding.inflate(LayoutInflater.from(ctx), this)

    fun setOnClickListener(onClickListener: ((view: View) -> Unit)) {
        binding.root.setOnClickListener(onClickListener)
    }

    fun updateFromText(fromDatePeriod: String) {
        binding.tvFromDate.text = fromDatePeriod
    }

    fun updateToText(toDatePeriod: String) {
        binding.tvToDate.text = toDatePeriod
    }
}
