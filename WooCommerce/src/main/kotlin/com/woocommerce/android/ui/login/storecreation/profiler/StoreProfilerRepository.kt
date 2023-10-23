package com.woocommerce.android.ui.login.storecreation.profiler

import com.google.gson.Gson
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.admin.WooAdminStore
import javax.inject.Inject

class StoreProfilerRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooAdminStore: WooAdminStore,
    private val gson: Gson,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val appPrefs: AppPrefsWrapper,
    private val tracker: AnalyticsTrackerWrapper
) {
    suspend fun fetchProfilerOptions(): ProfilerOptions = withContext(coroutineDispatchers.io) {
        gson.fromJson(PROFILER_OPTIONS_JSON, ProfilerOptions::class.java)
    }

    fun storeAnswers(
        siteId: Long,
        countryCode: String?,
        profilerAnswers: NewStore.ProfilerData?
    ) {
        appPrefs.storeCreationProfilerAnswers = gson.toJson(
            ProfilerAnswersCache(
                siteId = siteId,
                countryCode = countryCode,
                answers = profilerAnswers
            )
        )
    }

    suspend fun uploadAnswers() {
        val storedAnswers = appPrefs.storeCreationProfilerAnswers?.let {
            gson.fromJson(it, ProfilerAnswersCache::class.java)
        }
            ?.takeIf { selectedSite.get().siteId == it.siteId }
            ?: return

        wooAdminStore.updateOptions(
            site = selectedSite.get(),
            options = buildMap {
                storedAnswers.countryCode?.let {
                    put("woocommerce_default_country", it)
                }
                storedAnswers.answers?.let {
                    put(
                        key = "woocommerce_onboarding_profile",
                        value = mapOf(
                            "business_choice" to storedAnswers.answers.userCommerceJourneyKey,
                            "selling_platforms" to storedAnswers.answers.eCommercePlatformKeys,
                            "industry" to storedAnswers.answers.industryKey?.let { listOf(it) },
                            "is_store_country_set" to (storedAnswers.countryCode != null),
                        )
                    )
                }
            }
        ).let { result ->
            when {
                result.isError -> {
                    WooLog.w(
                        tag = WooLog.T.STORE_CREATION,
                        message = "Uploading Profiler Answers failed ${result.error.type}-${result.error.message}"
                    )
                }

                else -> {
                    WooLog.d(WooLog.T.STORE_CREATION, "Profiler Answers uploaded successfully")
                    tracker.track(
                        stat = AnalyticsEvent.SITE_CREATION_PROFILER_DATA,
                        properties = mapOf(
                            AnalyticsTracker.KEY_INDUSTRY_SLUG to storedAnswers.answers?.industryKey,
                            AnalyticsTracker.KEY_USER_COMMERCE_JOURNEY to storedAnswers.answers?.userCommerceJourneyKey,
                            AnalyticsTracker.KEY_ECOMMERCE_PLATFORMS to
                                storedAnswers.answers?.eCommercePlatformKeys?.joinToString(),
                            AnalyticsTracker.KEY_COUNTRY_CODE to storedAnswers.countryCode,
                            AnalyticsTracker.KEY_CHALLENGE to storedAnswers.answers?.challengeKey,
                            AnalyticsTracker.KEY_FEATURES to storedAnswers.answers?.featuresKey,
                        )
                    )
                    appPrefs.storeCreationProfilerAnswers = null
                }
            }
        }
    }

    private data class ProfilerAnswersCache(
        val siteId: Long,
        val countryCode: String?,
        val answers: NewStore.ProfilerData?
    )
}

private const val PROFILER_OPTIONS_JSON = """{
  "aboutMerchant": [
    {
      "id": 1,
      "value": "I'm just starting my business",
      "heading": "I'm just starting my business",
      "description": "",
      "tracks": "im_just_starting_my_business"
    },
    {
      "id": 2,
      "value": "I'm already selling, but not online",
      "heading": "I'm already selling, but not online",
      "description": "",
      "tracks": "im_already_selling_but_not_online"
    },
    {
      "id": 3,
      "value": "I'm already selling online",
      "heading": "I'm already selling online",
      "description": "",
      "tracks": "im_already_selling_online",
      "platforms": [
        { "value": "adobe-commerce", "label": "Adobe Commerce" },
        { "value": "amazon", "label": "Amazon" },
        { "value": "big-cartel", "label": "Big Cartel" },
        { "value": "big-commerce", "label": "Big Commerce" },
        { "value": "ebay", "label": "Ebay" },
        { "value": "ecwid", "label": "Ecwid" },
        { "value": "etsy", "label": "Etsy" },
        { "value": "facebook-marketplace", "label": "Facebook Marketplace" },
        { "value": "google-shopping", "label": "Google Shopping" },
        { "value": "magento", "label": "Magento" },
        { "value": "pinterest", "label": "Pinterest" },
        { "value": "shopify", "label": "Shopify" },
        { "value": "square", "label": "Square" },
        { "value": "squarespace", "label": "Squarespace" },
        { "value": "walmart", "label": "Walmart" },
        { "value": "wish", "label": "Wish" },
        { "value": "wix", "label": "Wix" },
        { "value": "wordPress", "label": "WordPress" }
      ]
    }
  ],
  "industries": [
    {
      "id": 0,
      "label": "Clothing and accessories",
      "key": "clothing_and_accessories"
    },
    {
      "id": 1,
      "label": "Health and beauty",
      "key": "health_and_beauty"
    },
    {
      "id": 2,
      "label": "Food and drink",
      "key": "food_and_drink"
    },
    {
      "id": 3,
      "label": "Home, furniture, and garden",
      "key": "home_furniture_and_garden"
    },
    {
      "id": 4,
      "label": "Education and learning",
      "key": "education_and_learning"
    },
    {
      "id": 5,
      "label": "Electronics and computers",
      "key": "electronics_and_computers"
    },
    {
      "id": 6,
      "label": "Other",
      "key": "other"
    }
  ]
}"""
