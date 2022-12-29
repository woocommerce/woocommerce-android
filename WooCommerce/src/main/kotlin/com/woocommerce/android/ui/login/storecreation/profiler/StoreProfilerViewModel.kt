package com.woocommerce.android.ui.login.storecreation.profiler

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.profiler.StoreProfilerViewModel.ProfilerOptionType.COMMERCE_JOURNEY
import com.woocommerce.android.ui.login.storecreation.profiler.StoreProfilerViewModel.ProfilerOptionType.ECOMMERCE_PLATFORM
import com.woocommerce.android.ui.login.storecreation.profiler.StoreProfilerViewModel.ProfilerOptionType.SITE_CATEGORY
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class StoreProfilerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    storeProfilerRepository: StoreProfilerRepository,
    private val newStore: NewStore,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedStateHandle) {
    private val allOptions = MutableStateFlow(emptyList<StoreProfilerOptionUi>())
    private val currentProfilerStepType = savedState.getStateFlow(this, SITE_CATEGORY)

    val storeProfilerContent: LiveData<ViewState> = combine(
        allOptions,
        currentProfilerStepType
    ) { allOptions, currentStep ->
        if (allOptions.isEmpty()) {
            LoadingState
        } else {
            StoreProfilerContent(
                storeName = newStore.data.name ?: "",
                title = resourceProvider.getString(getProfilerStepTitle(currentStep)),
                description = resourceProvider.getString(getProfilerStepDescription(currentStep)),
                options = allOptions.filter { it.type == currentStep }
            )
        }
    }.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_CATEGORY
            )
        )
        launch {
            val fetchedOptions = storeProfilerRepository.fetchProfilerOptions()
            allOptions.update {
                fetchedOptions.industries.map { it.toStoreProfilerOptionUi() } +
                    fetchedOptions.aboutMerchant.map { it.toStoreProfilerOptionUi() } +
                    fetchedOptions.aboutMerchant.first { it.platforms != null }.platforms!!
                        .map { it.toStoreProfilerOptionUi() }
            }
        }
    }

    fun onSkipPressed() {
        triggerEvent(NavigateToNextStep)
    }

    fun onArrowBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onContinueClicked() {
        when (currentProfilerStepType.value) {
            SITE_CATEGORY -> currentProfilerStepType.update { COMMERCE_JOURNEY }
            COMMERCE_JOURNEY -> currentProfilerStepType.update { ECOMMERCE_PLATFORM }
            ECOMMERCE_PLATFORM -> triggerEvent(NavigateToNextStep)
        }
    }

    fun onOptionSelected(option: StoreProfilerOptionUi) {
        allOptions.update {
            allOptions.value
                .map {
                    if (it.type == option.type) {
                        if (it.name == option.name) it.copy(isSelected = true)
                        else it.copy(isSelected = false)
                    } else {
                        it
                    }
                }
        }
        when (option.type) {
            SITE_CATEGORY -> newStore.update(category = option.name)
            COMMERCE_JOURNEY -> TODO()
            ECOMMERCE_PLATFORM -> TODO()
        }
    }

    private fun getProfilerStepDescription(currentStep: ProfilerOptionType): Int {
        val description = when (currentStep) {
            SITE_CATEGORY -> R.string.store_creation_store_profiler_industries_description
            COMMERCE_JOURNEY -> R.string.store_creation_store_profiler_journey_description
            ECOMMERCE_PLATFORM -> R.string.store_creation_store_profiler_platforms_description
        }
        return description
    }

    private fun getProfilerStepTitle(currentStep: ProfilerOptionType): Int {
        val title = when (currentStep) {
            SITE_CATEGORY -> R.string.store_creation_store_profiler_industries_title
            COMMERCE_JOURNEY -> R.string.store_creation_store_profiler_journey_title
            ECOMMERCE_PLATFORM -> R.string.store_creation_store_profiler_platforms_title
        }
        return title
    }

    private fun Industry.toStoreProfilerOptionUi() =
        StoreProfilerOptionUi(
            type = SITE_CATEGORY,
            name = label,
            isSelected = false
        )

    private fun AboutMerchant.toStoreProfilerOptionUi() =
        StoreProfilerOptionUi(
            type = COMMERCE_JOURNEY,
            name = value,
            isSelected = false
        )

    private fun Platform.toStoreProfilerOptionUi() =
        StoreProfilerOptionUi(
            type = ECOMMERCE_PLATFORM,
            name = label,
            isSelected = false
        )

    sealed class ViewState : Parcelable

    @Parcelize
    object LoadingState : ViewState()

    @Parcelize
    data class StoreProfilerContent(
        val storeName: String,
        val title: String,
        val description: String,
        val options: List<StoreProfilerOptionUi> = emptyList()
    ) : ViewState(), Parcelable

    @Parcelize
    data class StoreProfilerOptionUi(
        val type: ProfilerOptionType,
        val name: String,
        val isSelected: Boolean
    ) : Parcelable

    object NavigateToNextStep : MultiLiveEvent.Event()

    enum class ProfilerOptionType {
        SITE_CATEGORY,
        COMMERCE_JOURNEY,
        ECOMMERCE_PLATFORM
    }
}
