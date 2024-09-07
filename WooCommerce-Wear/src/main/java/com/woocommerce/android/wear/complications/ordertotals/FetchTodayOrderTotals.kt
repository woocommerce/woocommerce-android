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
    suspend operator fun invoke(): String? {
        return loginRepository.selectedSite
            ?.let { statsRepository.fetchRevenueStats(it) }
            ?.getOrNull()
            ?.parseTotal()
            ?.totalSales
            ?.format()
            ?: "N/A"
    }

    private fun Double.format(): String {
        if (this < 1000) return this.toString()
        val exp = (ln(this) / ln(1000.0)).toInt()
        return String.format(locale, "%.1f %c", this / 1000.0.pow(exp.toDouble()), "kMGTPE"[exp - 1])
    }
}
