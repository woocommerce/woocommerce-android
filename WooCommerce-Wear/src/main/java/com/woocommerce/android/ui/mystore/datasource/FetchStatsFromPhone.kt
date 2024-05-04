package com.woocommerce.android.ui.mystore.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.phone.PhoneConnectionRepository
import com.woocommerce.commons.wear.MessagePath.REQUEST_STATS
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FetchStatsFromPhone @Inject constructor(
    private val phoneRepository: PhoneConnectionRepository
) {
    suspend operator fun invoke(): Flow<MyStoreStatsData> {
        phoneRepository.sendMessage(REQUEST_STATS)
        return flowOf()
    }
}
