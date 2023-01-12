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
class StoreProfilerIndustriesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    analyticsTracker: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val storeProfilerRepository: StoreProfilerRepository,
    private val resourceProvider: ResourceProvider,
) : BaseStoreProfilerViewModel(savedStateHandle, newStore) {
    init {
        analyticsTracker.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_INDUSTRIES
            )
        )
        launch {
            val fetchedOptions = storeProfilerRepository.fetchProfilerOptions()
            profilerOptions.update {
                fetchedOptions.industries.map { it.toStoreProfilerOptionUi() }
            }
        }
    }

    override fun getProfilerStepDescription(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_industries_description)

    override fun getProfilerStepTitle(): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_industries_title)

    override fun onContinueClicked() {
        newStore.update(industry = profilerOptions.value.firstOrNull() { it.isSelected }?.name)
        triggerEvent(NavigateToCommerceJourneyStep)
    }

    private fun Industry.toStoreProfilerOptionUi() = StoreProfilerOptionUi(
        name = label,
        isSelected = newStore.data.industry == label
    )
}
