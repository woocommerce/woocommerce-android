package com.woocommerce.android.ui.blaze.creation.targets

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.DEVICE
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.INTEREST
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.LANGUAGE
import com.woocommerce.android.ui.blaze.creation.targets.BlazeTargetType.LOCATION
import com.woocommerce.android.ui.blaze.creation.targets.TargetSelectionViewState.SelectionItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignTargetSelectionViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    blazeRepository: BlazeRepository,
    savedStateHandle: SavedStateHandle,
) : TargetSelectionViewModel, ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignTargetSelectionFragmentArgs by savedStateHandle.navArgs()

    private val selectedIds = savedStateHandle.getStateFlow(viewModelScope, navArgs.selectedIds.toSet())

    private val items: Flow<List<SelectionItem>> = when (navArgs.targetType) {
        LANGUAGE -> blazeRepository.observeLanguages().map { languages ->
            languages.map { language ->
                SelectionItem(
                    id = language.code,
                    title = language.name
                )
            }
        }
        DEVICE -> blazeRepository.observeDevices().map { devices ->
            devices.map { device ->
                SelectionItem(
                    id = device.id,
                    title = device.name
                )
            }
        }
        INTEREST -> blazeRepository.observeInterests().map { interests ->
            interests.map { interest ->
                SelectionItem(
                    id = interest.id,
                    title = interest.description
                )
            }
        }
        LOCATION -> throw IllegalStateException("Location selection should not use this view model")
    }

    private val screenTitle: String
        get() = when (navArgs.targetType) {
            LANGUAGE -> resourceProvider.getString(R.string.blaze_campaign_preview_details_language)
            DEVICE -> resourceProvider.getString(R.string.blaze_campaign_preview_details_devices)
            INTEREST -> resourceProvider.getString(R.string.blaze_campaign_preview_details_interests)
            LOCATION -> throw IllegalStateException("Location selection should not use this view model")
        }

    override val viewState = combine(
        items,
        selectedIds,
    ) { items, selectedIds ->
        TargetSelectionViewState(
            items = items,
            selectedItems = selectedIds.map { id -> items.first { it.id == id } },
            title = screenTitle
        )
    }.asLiveData()

    override fun onItemToggled(item: SelectionItem) {
        selectedIds.update { selectedIds ->
            if (selectedIds.contains(item.id)) {
                selectedIds - item.id
            } else {
                selectedIds + item.id
            }
        }
    }

    override fun onAllButtonTapped() {
        selectedIds.update {
            if (it.size == viewState.value?.items?.size) {
                emptySet()
            } else {
                viewState.value?.items?.map { item -> item.id }?.toSet() ?: emptySet()
            }
        }
    }

    override fun onBackPressed() {
        triggerEvent(Exit)
    }

    override fun onSaveTapped() {
        // Empty selection set means all items are selected
        val result = if (selectedIds.value.size == viewState.value?.items?.size)
            emptyList()
        else
            selectedIds.value.toList()

        triggerEvent(ExitWithResult(TargetSelectionResult(navArgs.targetType, result)))
    }

    @Parcelize
    data class TargetSelectionResult(
        val targetType: BlazeTargetType,
        val selectedIds: List<String>
    ) : Parcelable
}
