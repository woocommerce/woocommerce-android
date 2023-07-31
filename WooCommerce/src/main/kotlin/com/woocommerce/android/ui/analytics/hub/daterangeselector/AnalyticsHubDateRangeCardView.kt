package com.woocommerce.android.ui.analytics.hub.daterangeselector

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.text.bold
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AnalyticsDateRangeCardViewBinding
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import java.util.Locale

class AnalyticsHubDateRangeCardView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    val binding = AnalyticsDateRangeCardViewBinding.inflate(LayoutInflater.from(ctx), this)

    fun setOnClickListener(onClickListener: ((view: View) -> Unit)) {
        binding.root.setOnClickListener(onClickListener)
    }

    fun updateSelectionTitle(selectionType: SelectionType) {
        binding.selectionTitle.text = context.getString(selectionType.localizedResourceId)
    }

    fun updateLastUpdateTimestamp(lastUpdateTimestamp: String) {
        with(binding.lastUpdateTimestamp) {
            lastUpdateTimestamp
                .takeIf { it.isNotEmpty() }
                ?.let {
                    isVisible = true
                    text = String.format(
                        Locale.getDefault(),
                        resources.getString(R.string.last_update_with_frequency),
                        lastUpdateTimestamp
                    )
                }
                ?: apply { isVisible = false }
        }
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
