package com.woocommerce.android.ui.login.storecreation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
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
            profilerData = profilerData ?: data.profilerData,
            country = country ?: data.country,
            siteId = siteId ?: data.siteId,
            planProductId = planProductId ?: data.planProductId,
            planPathSlug = planPathSlug ?: data.planPathSlug
        )
    }

    fun clear() {
        data = NewStoreData()
    }

    @Parcelize
    data class NewStoreData(
        val name: String? = null,
        val domain: String? = null,
        val profilerData: ProfilerData? = null,
        val country: Country? = null,
        val siteId: Long? = null,
        val planProductId: Int? = null,
        val planPathSlug: String? = null
    ) : Parcelable

    @Parcelize
    data class Country(
        val name: String,
        val code: String,
    ) : Parcelable

    @Parcelize
    data class ProfilerData(
        val industryLabel: String? = null,
        val industryKey: String? = null,
        val industryGroupKey: String? = null,
        val userCommerceJourneyKey: String? = null,
        val eCommercePlatformKeys: List<String> = emptyList(),
        val challengeKey: String? = null,
        val featuresKey: String? = null,
    ) : Parcelable
}
