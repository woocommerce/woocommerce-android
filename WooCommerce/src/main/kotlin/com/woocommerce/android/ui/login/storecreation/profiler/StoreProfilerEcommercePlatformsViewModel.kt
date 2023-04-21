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
) : BaseStoreProfilerViewModel(savedStateHandle, newStore) {
    override val hasSearchableContent: Boolean
        get() = false

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

    override fun getProfilerStepTitle(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_platforms_title)

    override fun onOptionSelected(option: StoreProfilerOptionUi) {
        profilerOptions.update { currentOptions ->
            currentOptions.map {
                if (option.name == it.name) it.copy(isSelected = !it.isSelected)
                else it
            }
        }
    }

    override fun onContinueClicked() {
        newStore.update(
            profilerData = (newStore.data.profilerData ?: NewStore.ProfilerData())
                .copy(
                    eCommercePlatformKeys = profilerOptions.value
                        .filter { it.isSelected }
                        .map { it.key }
                )
        )
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
