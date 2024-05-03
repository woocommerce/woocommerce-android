package com.woocommerce.android.ui.mystore

import com.woocommerce.android.ui.login.LoginRepository
import org.wordpress.android.fluxc.store.WCStatsStore
import java.util.Calendar
import java.util.Locale

class StatsRepository(
    private val loginRepository: LoginRepository,
    private val wcStatsStore: WCStatsStore,
) {
    fun fetchStoreStats() {
        val locale = Locale.getDefault()
        val selectedSite = loginRepository.currentSite ?: return
        val todayRange = TodayRangeData(
            selectedSite = selectedSite,
            locale = locale,
            referenceCalendar = Calendar.getInstance()
        )
    }
}
