package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StoreProfilerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedStateHandle) {
    private val _storeProfilerState = savedState.getStateFlow(
        scope = this,
        initialValue = StoreProfilerState(
            storeName = newStore.data.name ?: "",
            selectedCategory = null,
            categories = emptyList()
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


    data class StoreProfilerState(
        val storeName: String? = null,
        val selectedCategory: String? = null,
        val categories: List<Category> = emptyList()
    )

    data class Category(
        val name: String,
        val isSelected: Boolean
    )

    object NavigateToNextStep : MultiLiveEvent.Event()
}
