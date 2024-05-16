package com.woocommerce.android.ui.stats.datasource

import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.util.combineWithTimeout
import com.woocommerce.commons.wear.MessagePath.REQUEST_STATS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

class FetchStatsFromPhone @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(): Flow<StoreStatsRequest?> {
        phoneRepository.sendMessage(REQUEST_STATS)
        return statsRepository.observeStatsDataChanges().combineWithTimeout { statsData, isTimeout ->
            when {
                statsData?.isFinished == true -> statsData
                isTimeout -> StoreStatsRequest.Error
                else -> null
            }
        }.filterNotNull()
    }
}
