package com.woocommerce.android.ui.login.storecreation.mystoresummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin.STORE_CREATION
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.util.EmojiUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class MyStoreSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    newStore: NewStore,
    analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    emojiUtils: EmojiUtils
) : ScopedViewModel(savedStateHandle) {

    val viewState = MutableStateFlow(
        MyStoreSummaryState(
            name = newStore.data.name,
            domain = newStore.data.domain ?: "",
            industry = newStore.data.profilerData?.industryLabel,
            country = Country(
                name = newStore.data.country?.name ?: "",
                emojiFlag = emojiUtils.countryCodeToEmojiFlag(newStore.data.country?.code ?: "")
            )
        )
    ).asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_SUMMARY
            )
        )

        val newStoreProfilerData = newStore.data.profilerData
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.SITE_CREATION_PROFILER_DATA,
            properties = mapOf(
                AnalyticsTracker.KEY_INDUSTRY_SLUG to newStoreProfilerData?.industryKey,
                AnalyticsTracker.KEY_USER_COMMERCE_JOURNEY to newStoreProfilerData?.userCommerceJourneyKey,
                AnalyticsTracker.KEY_ECOMMERCE_PLATFORMS to newStoreProfilerData?.eCommercePlatformKeys?.joinToString(),
                AnalyticsTracker.KEY_COUNTRY_CODE to newStore.data.country?.code,
            )
        )
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onHelpPressed() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(STORE_CREATION))
    }

    fun onContinueClicked() {
        triggerEvent(NavigateToNextStep)
    }

    data class MyStoreSummaryState(
        val name: String? = null,
        val domain: String,
        val industry: String? = null,
        val country: Country? = null,
    )

    data class Country(
        val name: String,
        val emojiFlag: String
    )

    object NavigateToNextStep : MultiLiveEvent.Event()
}
