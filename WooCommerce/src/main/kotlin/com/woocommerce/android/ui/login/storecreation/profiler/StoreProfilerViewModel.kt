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
    private var allOptions: List<StoreProfilerOptionUi> = emptyList()
    private val _storeProfilerContent = savedState.getStateFlow<ViewState>(
        scope = this,
        initialValue = Loading
    )

    val storeProfilerContent: LiveData<ViewState> = _storeProfilerContent.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_CATEGORY
            )
        )
        launch {
            loadProfilerOptions(storeProfilerRepository)
            _storeProfilerContent.value = StoreProfilerContent(
                storeName = newStore.data.name ?: "",
                title = resourceProvider.getString(R.string.store_creation_store_categories_title),
                description = resourceProvider.getString(R.string.store_creation_store_categories_subtitle),
                options = allOptions.filter { it.type == SITE_CATEGORY }
            )
        }
    }

    fun onSkipPressed() {
        triggerEvent(NavigateToNextStep)
    }

    fun onArrowBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onContinueClicked() {
//        _storeProfilerContent.value.options
//            .firstOrNull { it.isSelected }
//            ?.name
//            ?.let {
//                newStore.update(category = it)
//            }
//

        triggerEvent(NavigateToNextStep)
    }

    fun onOptionSelected(option: StoreProfilerOptionUi) {
        allOptions = allOptions.map {
            if (it.type == option.type) {
                if (it.name == option.name) it.copy(isSelected = true)
                else it.copy(isSelected = false)
            } else {
                it
            }
        }
        _storeProfilerContent.value = StoreProfilerContent(
            storeName = newStore.data.name ?: "",
            title = resourceProvider.getString(R.string.store_creation_store_categories_title),
            description = resourceProvider.getString(R.string.store_creation_store_categories_subtitle),
            options = allOptions
        )
    }

    private suspend fun loadProfilerOptions(storeProfilerRepository: StoreProfilerRepository) {
        val profilerOptions = storeProfilerRepository.fetchProfilerOptions()

        allOptions =
            profilerOptions.industries.map { it.toStoreProfilerOptionUi() } +
                profilerOptions.aboutMerchant.map { it.toStoreProfilerOptionUi() } +
                profilerOptions.aboutMerchant.first { it.platforms != null }
                    .platforms!!
                    .map { it.toStoreProfilerOptionUi() }
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
    object Loading : ViewState()

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
