package com.woocommerce.android.ui.blaze.creation.targets

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeRepository
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
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignTargetSelectionFragmentArgs by savedStateHandle.navArgs()

    private val items: Flow<List<TargetItem>> = when (navArgs.targetType) {
        BlazeTargetType.LANGUAGE -> blazeRepository.observeLanguages().map { languages ->
            languages.map { language ->
                TargetItem(
                    id = language.code,
                    value = language.name
                )
            }
        }
        BlazeTargetType.DEVICE -> blazeRepository.observeDevices().map { devices ->
            devices.map { device ->
                TargetItem(
                    id = device.id,
                    value = device.name
                )
            }
        }
        else -> blazeRepository.observeInterests().map { interests ->
            interests.map { interest ->
                TargetItem(
                    id = interest.id,
                    value = interest.description
                )
            }
        }
    }

    private val selectedIds = savedStateHandle.getStateFlow(viewModelScope, navArgs.selectedIds.toSet())

    val viewState = combine(items, selectedIds) { items, selectedIds ->
        ViewState(
            items = items,
            selectedItems = selectedIds.map { id -> items.first { it.id == id } },
            title = when (navArgs.targetType) {
                BlazeTargetType.LANGUAGE -> resourceProvider.getString(R.string.blaze_campaign_preview_details_language)
                BlazeTargetType.DEVICE -> resourceProvider.getString(R.string.blaze_campaign_preview_details_devices)
                else -> resourceProvider.getString(R.string.blaze_campaign_preview_details_interests)
            }
        )
    }.asLiveData()

    fun onItemTapped(item: TargetItem) {
        selectedIds.update { selectedIds ->
            if (selectedIds.contains(item.id)) {
                selectedIds - item.id
            } else {
                selectedIds + item.id
            }
        }
    }

    fun onAllButtonTapped() {
        selectedIds.update { emptySet() }
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onSaveTapped() {
        // Empty selection set means all items are selected
        val result = if (selectedIds.value.size == viewState.value?.items?.size)
            emptyList()
        else
            selectedIds.value.toList()
        triggerEvent(ExitWithResult(TargetSelectionResult(navArgs.targetType, result)))
    }

    data class TargetItem(
        val id: String,
        val value: String,
    )

    data class ViewState(
        val items: List<TargetItem>,
        val selectedItems: List<TargetItem>,
        val title: String
    )

    @Parcelize
    data class TargetSelectionResult(
        val targetType: BlazeTargetType,
        val selectedIds: List<String>
    ) : Parcelable
}
