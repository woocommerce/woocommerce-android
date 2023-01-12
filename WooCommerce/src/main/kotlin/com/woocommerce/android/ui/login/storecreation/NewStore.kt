package com.woocommerce.android.ui.login.storecreation

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewStore @Inject constructor() {
    var data = NewStoreData()
        private set

    fun update(
        name: String? = null,
        domain: String? = null,
        profilerData: ProfilerData? = null,
        country: Country? = null,
        siteId: Long? = null,
        planProductId: Int? = null,
        planPathSlug: String? = null
    ) {
        data = data.copy(
            name = name ?: data.name,
            domain = domain ?: data.domain,
            profilerData = profilerData,
            country = country ?: data.country,
            siteId = siteId ?: data.siteId,
            planProductId = planProductId ?: data.planProductId,
            planPathSlug = planPathSlug ?: data.planPathSlug
        )
    }

    fun clear() {
        data = NewStoreData()
    }

    data class NewStoreData(
        val name: String? = null,
        val domain: String? = null,
        val profilerData: ProfilerData? = null,
        val country: Country? = null,
        val siteId: Long? = null,
        val planProductId: Int? = null,
        val planPathSlug: String? = null
    )

    data class Country(
        val name: String,
        val code: String,
    )

    /**
     * This data is meant to be temporary until we have an API ready to send
     */
    data class ProfilerData(
        val industryLabel: String? = null,
        val industryKey: String? = null,
        val industryGroupKey: String? = null,
        val userCommerceJourneyKey: String? = null,
        val eCommercePlatformKey: String? = null,
    )
}
