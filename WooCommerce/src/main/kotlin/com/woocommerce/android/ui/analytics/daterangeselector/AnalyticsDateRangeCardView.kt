package com.woocommerce.android.ui.analytics.daterangeselector

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.text.bold
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
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

    fun updateSelectionTitle(selectionTitle: String) {
        binding.selectionTitle.text = selectionTitle
    }

    fun updatePreviousRange(previousRange: String) {
        SpannableStringBuilder()
            .append(resources.getString(R.string.date_compared_to))
            .append(" ")
            .bold { append(previousRange) }
            .let { binding.previousRangeDescription.text = it }
    }

    fun updateCurrentRange(currentRange: String) {
        binding.currentRangeDescription.text = currentRange
    }
}
