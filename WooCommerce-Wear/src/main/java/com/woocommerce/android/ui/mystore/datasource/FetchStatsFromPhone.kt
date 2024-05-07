package com.woocommerce.android.ui.mystore.datasource

import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.ui.login.ObserveLoginRequest.Companion.TIMEOUT_MILLIS
import com.woocommerce.android.ui.mystore.stats.StatsRepository
import com.woocommerce.commons.wear.MessagePath.REQUEST_STATS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull

class FetchStatsFromPhone @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository,
    private val statsRepository: StatsRepository
) {
    suspend operator fun invoke(): Flow<MyStoreStatsRequest?> {
        phoneRepository.sendMessage(REQUEST_STATS)
        return combine(statsRepository.observeStatsDataChanges(), timeoutFlow) { statsData, isTimeout ->
            when {
                statsData?.isFinished == true -> statsData
                isTimeout -> MyStoreStatsRequest.Error
                else -> null
            }
        }.filterNotNull()
    }

    private val timeoutFlow: Flow<Boolean>
        get() = flow {
            emit(true)
            delay(TIMEOUT_MILLIS)
            emit(false)
        }
}
