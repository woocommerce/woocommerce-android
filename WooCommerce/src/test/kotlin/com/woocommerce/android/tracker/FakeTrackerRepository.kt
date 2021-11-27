package com.woocommerce.android.tracker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel

class FakeTrackerRepository : TrackerRepository {
    val inMemoryState = MutableStateFlow<Map<SiteModel, Long>>(emptyMap())

    override fun observeLastSendingDate(site: SiteModel): Flow<Long> {
        return inMemoryState.map {
            it.getOrDefault(site, 0)
        }
    }

    override suspend fun updateLastSendingDate(site: SiteModel, lastUpdateMillis: Long) {
        inMemoryState.value += (site to lastUpdateMillis)
    }
}
