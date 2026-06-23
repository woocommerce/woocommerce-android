package com.woocommerce.android.ui.woopos.orders.details.refund

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosV4RefundAvailabilityCache @Inject constructor() {
    private val availabilityBySiteId = ConcurrentHashMap<Long, Boolean>()

    fun isV4Available(siteId: Long): Boolean? = availabilityBySiteId[siteId]

    fun markV4Available(siteId: Long) {
        availabilityBySiteId[siteId] = true
    }

    fun markV4Unavailable(siteId: Long) {
        availabilityBySiteId[siteId] = false
    }
}
