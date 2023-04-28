package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreProfilerCommerceJourneyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val storeProfilerRepository: StoreProfilerRepository,
    private val resourceProvider: ResourceProvider,
) : BaseStoreProfilerViewModel(savedStateHandle, newStore) {
    private companion object {
        const val STARTING_BUSINESS_KEY = "im_just_starting_my_business"
        const val NOT_SELLING_ONLINE_KEY = "im_already_selling_but_not_online"
        const val SELLING_ONLINE_KEY = "im_already_selling_online"
    }

    private var alreadySellingOnlineOption: StoreProfilerOptionUi? = null
    override val hasSearchableContent: Boolean
        get() = false

    init {
        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_COMMERCE_JOURNEY
            )
        )
        launch {
            val fetchedOptions = storeProfilerRepository.fetchProfilerOptions()
            profilerOptions.update {
                fetchedOptions.aboutMerchant.map { it.toStoreProfilerOptionUi() }
            }
            alreadySellingOnlineOption = fetchedOptions.aboutMerchant
                .firstOrNull { !it.platforms.isNullOrEmpty() }
                ?.toStoreProfilerOptionUi()
        }
    }

    override fun getProfilerStepDescription(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_journey_description)

    override fun getProfilerStepTitle(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_journey_title)

    override fun onContinueClicked() {
        newStore.update(
            profilerData = (newStore.data.profilerData ?: NewStore.ProfilerData())
                .copy(
                    userCommerceJourneyKey = profilerOptions.value.firstOrNull { it.isSelected }?.key
                )
        )
        when (alreadySellingOnlineSelected()) {
            true -> triggerEvent(NavigateToEcommercePlatformsStep)
            false -> triggerEvent(NavigateToNextStep)
        }
    }

    override fun onSkipPressed() {
        super.onSkipPressed()

        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_PROFILER_QUESTION_SKIPPED,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_COMMERCE_JOURNEY
            )
        )
    }

    private fun AboutMerchant.toStoreProfilerOptionUi() = StoreProfilerOptionUi(
        name = getLocalizedCommerceJourney(tracks),
        key = tracks,
        isSelected = newStore.data.profilerData?.userCommerceJourneyKey == tracks,
    )

    private fun alreadySellingOnlineSelected() = profilerOptions.value
        .firstOrNull { it.isSelected && it.name == alreadySellingOnlineOption?.name } != null

    private fun getLocalizedCommerceJourney(commerceJourneyKey: String): String =
        resourceProvider.getString(
            when (commerceJourneyKey) {
                STARTING_BUSINESS_KEY -> R.string.store_creation_profiler_merchant_journey_starting_business
                NOT_SELLING_ONLINE_KEY -> R.string.store_creation_profiler_merchant_journey_selling_not_online
                SELLING_ONLINE_KEY -> R.string.store_creation_profiler_merchant_journey_selling_online
                else -> R.string.store_creation_profiler_merchant_journey_starting_business
            }
        )
}
