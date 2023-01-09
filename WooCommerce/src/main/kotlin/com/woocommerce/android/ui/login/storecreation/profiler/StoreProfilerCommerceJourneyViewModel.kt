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
    analyticsTracker: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val storeProfilerRepository: StoreProfilerRepository,
    private val resourceProvider: ResourceProvider,
) : BaseStoreProfilerViewModel(savedStateHandle, newStore) {
    private var alreadySellingOnlineOption: StoreProfilerOptionUi? = null

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
        newStore.update(commerceJourney = profilerOptions.value.firstOrNull() { it.isSelected }?.name)
        when (alreadySellingOnlineSelected()) {
            true -> triggerEvent(NavigateToEcommercePlatformsStep)
            false -> triggerEvent(NavigateToDomainPickerStep)
        }
    }

    private fun AboutMerchant.toStoreProfilerOptionUi() = StoreProfilerOptionUi(
        name = value,
        isSelected = newStore.data.industry == value,
    )

    private fun alreadySellingOnlineSelected() = profilerOptions.value
        .firstOrNull { it.isSelected && it.name == alreadySellingOnlineOption?.name } != null
}
