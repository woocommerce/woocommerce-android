package com.woocommerce.android.tracker

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.SiteModel

interface TrackerRepository {
    fun observeLastSendingDate(site: SiteModel): Flow<Long>

    suspend fun updateLastSendingDate(site: SiteModel, lastUpdateMillis: Long)
}
