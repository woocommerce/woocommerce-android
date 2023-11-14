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
class StoreProfilerEcommercePlatformsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val storeProfilerRepository: StoreProfilerRepository,
    private val resourceProvider: ResourceProvider,
) : BaseStoreProfilerViewModel(savedStateHandle, newStore, storeProfilerRepository) {
    override val hasSearchableContent: Boolean
        get() = false
    override val isMultiChoice: Boolean = true

    init {
        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_ECOMMERCE_PLATFORMS
            )
        )
        launch {
            val fetchedOptions = storeProfilerRepository.fetchProfilerOptions()
            profilerOptions.update {
                fetchedOptions.aboutMerchant
                    .firstOrNull { !it.platforms.isNullOrEmpty() }
                    ?.platforms
                    ?.map { it.toStoreProfilerOptionUi() } ?: emptyList()
            }
        }
    }

    private fun Platform.toStoreProfilerOptionUi() =
        StoreProfilerOptionUi(
            name = label,
            key = value,
            isSelected = newStore.data.profilerData?.eCommercePlatformKeys?.any { it == value } == true
        )

    override fun getProfilerStepDescription(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_platforms_description)

    override fun getMainButtonText(): String =
        resourceProvider.getString(R.string.continue_button)

    override fun getProfilerStepTitle(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_platforms_title)

    override fun saveStepAnswer() {
        newStore.update(
            profilerData = (newStore.data.profilerData ?: NewStore.ProfilerData())
                .copy(
                    eCommercePlatformKeys = profilerOptions.value
                        .filter { it.isSelected }
                        .map { it.key }
                )
        )
    }

    override fun moveForward() {
        triggerEvent(NavigateToNextStep)
    }

    override fun onSkipPressed() {
        super.onSkipPressed()

        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_PROFILER_QUESTION_SKIPPED,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_ECOMMERCE_PLATFORMS
            )
        )
    }
}
