package com.woocommerce.android.ui.stats.datasource

import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.ui.login.ObserveLoginRequest.Companion.TIMEOUT_MILLIS
import com.woocommerce.commons.wear.MessagePath.REQUEST_STATS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FetchStatsFromPhone @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(): Flow<StoreStatsRequest?> {
        phoneRepository.sendMessage(REQUEST_STATS)
        return combine(statsRepository.observeStatsDataChanges(), timeoutFlow) { statsData, isTimeout ->
            when {
                statsData?.isFinished == true -> statsData
                isTimeout -> StoreStatsRequest.Error
                else -> null
            }
        }.filterNotNull()
    }

    private val timeoutFlow: Flow<Boolean>
        get() = flow {
            emit(false)
            delay(TIMEOUT_MILLIS)
            emit(true)
        }
}
