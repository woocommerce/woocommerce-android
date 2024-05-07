package com.woocommerce.android.ui.mystore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.android.ui.login.ObserveLoginRequest.Companion.TIMEOUT_MILLIS
import com.woocommerce.android.ui.mystore.stats.StatsRepository
import com.woocommerce.commons.wear.MessagePath.REQUEST_STATS
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FetchStatsFromPhone @Inject constructor(
    @DataStoreQualifier(DataStoreType.STATS) private val statsDataStore: DataStore<Preferences>,
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
        }
    }

    private val timeoutFlow: Flow<Boolean>
        get() = flow {
            emit(true)
            delay(TIMEOUT_MILLIS)
            emit(false)
        }
}
