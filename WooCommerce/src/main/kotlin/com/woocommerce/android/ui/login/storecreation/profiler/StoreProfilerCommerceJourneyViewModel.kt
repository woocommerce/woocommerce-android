package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.profiler.BaseStoreProfilerViewModel.ProfilerOptionType.COMMERCE_JOURNEY
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreProfilerCommerceJourneyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val storeProfilerRepository: StoreProfilerRepository,
    private val resourceProvider: ResourceProvider,
) : BaseStoreProfilerViewModel(savedStateHandle, newStore) {
    override val profilerStep: ProfilerOptionType = COMMERCE_JOURNEY

    private var alreadySellingOnlineOption: StoreProfilerOptionUi? = null

    init {
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

    override fun getProfilerStepDescription(currentStep: ProfilerOptionType): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_journey_description)

    override fun getProfilerStepTitle(currentStep: ProfilerOptionType): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_journey_title)

    override fun onContinueClicked() {
        newStore.update(commercJourney = profilerOptions.value.firstOrNull() { it.isSelected }?.name)
        when (alreadySellingOnlineSelected()) {
            true -> triggerEvent(NavigateToEcommercePlatformsStep)
            false -> triggerEvent(NavigateToDomainPickerStep)
        }
    }

    private fun AboutMerchant.toStoreProfilerOptionUi() = StoreProfilerOptionUi(
        type = profilerStep,
        name = value,
        isSelected = newStore.data.industry == value,
    )

    private fun alreadySellingOnlineSelected() = profilerOptions.value
        .firstOrNull { it.isSelected && it.name == alreadySellingOnlineOption?.name } != null
}
