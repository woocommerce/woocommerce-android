package com.woocommerce.android.wear.complications.ordertotals

import android.icu.text.CompactDecimalFormat
import com.woocommerce.android.wear.ui.login.LoginRepository
import com.woocommerce.android.wear.ui.stats.datasource.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class FetchTodayOrderTotals @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val statsRepository: StatsRepository,
    private val loginRepository: LoginRepository,
    private val decimalFormat: CompactDecimalFormat
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
            ?.let { decimalFormat.format(it) }
            ?: DEFAULT_EMPTY_VALUE
    }

    companion object {
        const val DEFAULT_EMPTY_VALUE = "N/A"
    }
}
