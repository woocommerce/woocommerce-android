package com.woocommerce.android.wear.complications.ordertotals

import android.icu.text.CompactDecimalFormat
import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.android.wear.ui.stats.datasource.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale
import javax.inject.Inject

class FetchTodayOrderTotals @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val statsRepository: StatsRepository,
    private val loginRepository: LoginRepository,
    private val locale: Locale
) {
    suspend operator fun invoke(): String {
        val site = coroutineScope.async {
            loginRepository.selectedSiteFlow
                .filterNotNull()
                .firstOrNull()
        }.await()

        return site?.let { statsRepository.fetchRevenueStats(it) }
            ?.getOrNull()
            ?.parseTotal()
            ?.totalSales
            ?.format()
            ?: DEFAULT_EMPTY_VALUE
    }

    private fun Double.format(): String {
        return CompactDecimalFormat.getInstance(
            locale,
            CompactDecimalFormat.CompactStyle.SHORT
        ).format(this)
    }

    companion object {
        const val DEFAULT_EMPTY_VALUE = "N/A"
    }
}
