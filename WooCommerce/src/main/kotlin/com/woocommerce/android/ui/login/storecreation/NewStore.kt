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
        industry: String? = null,
        commerceJourney: String? = null,
        eCommercePlatform: String? = null,
        country: Country? = null,
        siteId: Long? = null,
        planProductId: Int? = null,
        planPathSlug: String? = null
    ) {
        data = data.copy(
            name = name ?: data.name,
            domain = domain ?: data.domain,
            industry = industry ?: data.industry,
            userJourney = commerceJourney ?: data.userJourney,
            platform = eCommercePlatform ?: data.platform,
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
        val industry: String? = null,
        val userJourney: String? = null,
        val platform: String? = null,
        val country: Country? = null,
        val siteId: Long? = null,
        val planProductId: Int? = null,
        val planPathSlug: String? = null
    )

    data class Country(
        val name: String,
        val code: String,
    )
}
