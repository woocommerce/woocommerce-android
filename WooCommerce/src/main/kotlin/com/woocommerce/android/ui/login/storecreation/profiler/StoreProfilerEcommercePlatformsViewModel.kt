package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.profiler.BaseStoreProfilerViewModel.ProfilerOptionType.COMMERCE_JOURNEY
import com.woocommerce.android.ui.login.storecreation.profiler.BaseStoreProfilerViewModel.ProfilerOptionType.ECOMMERCE_PLATFORM
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreProfilerEcommercePlatformsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val storeProfilerRepository: StoreProfilerRepository,
    private val resourceProvider: ResourceProvider,
) : BaseStoreProfilerViewModel(savedStateHandle, newStore) {
    override val profilerStep: ProfilerOptionType = COMMERCE_JOURNEY

    init {
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
            type = ECOMMERCE_PLATFORM,
            name = label,
            isSelected = newStore.data.industry == label
        )

    override fun getProfilerStepDescription(currentStep: ProfilerOptionType): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_platforms_description)

    override fun getProfilerStepTitle(currentStep: ProfilerOptionType): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_platforms_title)

    override fun onContinueClicked() {
        newStore.update(eCommercePlatform = profilerOptions.value.firstOrNull() { it.isSelected }?.name)
        triggerEvent(NavigateToDomainPickerStep)
    }
}
