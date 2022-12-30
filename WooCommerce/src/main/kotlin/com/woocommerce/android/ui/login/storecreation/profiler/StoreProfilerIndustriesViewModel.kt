package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.profiler.BaseStoreProfilerViewModel.ProfilerOptionType.SITE_INDUSTRY
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreProfilerIndustriesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val storeProfilerRepository: StoreProfilerRepository,
    private val resourceProvider: ResourceProvider,
) : BaseStoreProfilerViewModel(savedStateHandle, newStore) {
    override val profilerStep: ProfilerOptionType = SITE_INDUSTRY

    init {
        launch {
            val fetchedOptions = storeProfilerRepository.fetchProfilerOptions()
            profilerOptions.update {
                fetchedOptions.industries.map { it.toStoreProfilerOptionUi() }
            }
        }
    }

    override fun getProfilerStepDescription(currentStep: ProfilerOptionType): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_industries_description)

    override fun getProfilerStepTitle(currentStep: ProfilerOptionType): String =
        resourceProvider.getString(R.string.store_creation_store_profiler_industries_title)

    override fun onContinueClicked() {
        newStore.update(industry = profilerOptions.value.firstOrNull() { it.isSelected }?.name)
        triggerEvent(NavigateToCommerceJourneyStep)
    }

    private fun Industry.toStoreProfilerOptionUi() = StoreProfilerOptionUi(
        type = profilerStep, name = label, isSelected = newStore.data.industry == label
    )
}
