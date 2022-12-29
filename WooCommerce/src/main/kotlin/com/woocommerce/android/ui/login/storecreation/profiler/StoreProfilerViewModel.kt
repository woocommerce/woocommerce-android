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
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class StoreProfilerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {
    private val _storeProfilerState = savedState.getStateFlow(
        scope = this,
        initialValue = StoreProfilerState(
            storeName = newStore.data.name ?: "",
            title = resourceProvider.getString(R.string.store_creation_store_categories_title),
            description = resourceProvider.getString(R.string.store_creation_store_categories_subtitle),
            options = CATEGORIES
        )
    )

    val storeProfilerState: LiveData<StoreProfilerState> = _storeProfilerState.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_CATEGORY
            )
        )
    }

    fun onSkipPressed() {
        triggerEvent(NavigateToNextStep)
    }

    fun onArrowBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onContinueClicked() {
        _storeProfilerState.value.options
            .firstOrNull { it.isSelected }
            ?.name
            ?.let {
                newStore.update(category = it)
            }
        triggerEvent(NavigateToNextStep)
    }

    fun onCategorySelected(category: StoreProfilerOptionUi) {
        val updatedCategories = _storeProfilerState.value.options.map {
            if (it == category) {
                it.copy(isSelected = true)
            } else {
                it.copy(isSelected = false)
            }
        }
        _storeProfilerState.value = _storeProfilerState.value.copy(
            options = updatedCategories
        )
    }

    @Parcelize
    data class StoreProfilerState(
        val storeName: String,
        val title: String,
        val description: String,
        val options: List<StoreProfilerOptionUi> = emptyList()
    ) : Parcelable

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
