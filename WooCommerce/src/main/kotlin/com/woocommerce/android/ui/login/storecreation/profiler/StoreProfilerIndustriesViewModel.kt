package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.NewStore.ProfilerData
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreProfilerIndustriesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val storeProfilerRepository: StoreProfilerRepository,
    private val resourceProvider: ResourceProvider,
) : BaseStoreProfilerViewModel(savedStateHandle, newStore, storeProfilerRepository) {
    private companion object {
        const val DEFAULT_INDUSTRY_STRING_KEY_PREFIX = "store_creation_profiler_industry_"
    }

    private var industries: List<Industry> = emptyList()
    override val hasSearchableContent: Boolean
        get() = false

    init {
        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_INDUSTRIES
            )
        )
        launch {
            val fetchedOptions = storeProfilerRepository.fetchProfilerOptions()
            industries = fetchedOptions.industries
            profilerOptions.update {
                industries.map { it.toStoreProfilerOptionUi() }
            }
        }
    }

    override fun getProfilerStepDescription(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_industries_description)

    override fun getMainButtonText(): String =
        resourceProvider.getString(R.string.continue_button)

    override fun getProfilerStepTitle(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_industries_title)

    override fun saveStepAnswer() {
        val selectedOption = profilerOptions.value.firstOrNull { it.isSelected }
        val selectedIndustry = industries.firstOrNull { selectedOption?.key == it.key }
        newStore.update(
            profilerData = (newStore.data.profilerData ?: ProfilerData())
                .copy(
                    industryLabel = selectedOption?.name,
                    industryKey = selectedIndustry?.key
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
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_INDUSTRIES
            )
        )
    }

    private fun Industry.toStoreProfilerOptionUi() = StoreProfilerOptionUi(
        name = getLocalisedIndustryString(stringKeySuffix = key, fallbackValue = label),
        key = key,
        isSelected = newStore.data.profilerData?.industryKey == key
    )

    private fun getLocalisedIndustryString(stringKeySuffix: String, fallbackValue: String): String {
        val industryStringKey = DEFAULT_INDUSTRY_STRING_KEY_PREFIX + stringKeySuffix
        val stringRes = resourceProvider.getStringResFromStringName(industryStringKey)
        return stringRes?.let {
            resourceProvider.getString(it)
        } ?: fallbackValue
    }
}
