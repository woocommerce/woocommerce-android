package com.woocommerce.android.wear.complications.ordertotals

import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.android.wear.ui.stats.datasource.StatsRepository
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ln
import kotlin.math.pow

class FetchTodayOrderTotals @Inject constructor(
    private val statsRepository: StatsRepository,
    private val loginRepository: LoginRepository,
    private val locale: Locale
) {
    suspend operator fun invoke(): String {
        return loginRepository.selectedSite
            ?.let { statsRepository.fetchRevenueStats(it) }
            ?.getOrNull()
            ?.parseTotal()
            ?.totalSales
            ?.format()
            ?: DEFAULT_EMPTY_VALUE
    }

    private fun Double.format(): String {
        if (this < 1000) return this.toString()
        val exp = (ln(this) / ln(1000.0)).toInt()
        val formattingValue = this / 1000.0.pow(exp.toDouble())
        return String.format(locale, NUMBER_FORMAT, formattingValue, UNIT_SUFFIX[exp - 1])
    }

    companion object {
        const val NUMBER_FORMAT = "%.1f %c"
        const val UNIT_SUFFIX = "kMGTPE"
        const val DEFAULT_EMPTY_VALUE = "N/A"
    }
}
