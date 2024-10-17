package com.woocommerce.android.ui.blaze.creation.objective

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
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
class BlazeCampaignObjectiveViewModel @Inject constructor(
    private val blazeRepository: BlazeRepository,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: BlazeCampaignObjectiveFragmentArgs by savedStateHandle.navArgs()

    private val selectedId = savedStateHandle.getNullableStateFlow(
        scope = viewModelScope,
        initialValue = navArgs.selectedObjectiveId,
        clazz = String::class.java,
        key = "selectedId"
    )
    private val storeObjectiveSwitchState = savedState.getStateFlow(
        scope = this,
        initialValue = blazeRepository.isStoreObjectiveEnabled(),
        key = "storeObjectiveSwitchState"
    )

    private val items: Flow<List<ObjectiveItem>> =
        blazeRepository.observeObjectives().map { objectives ->
            objectives.map { objective ->
                ObjectiveItem(objective.id, objective.title, objective.description, objective.suitableForDescription)
            }
        }

    val viewState = combine(
        items,
        selectedId,
        storeObjectiveSwitchState
    ) { items, selectedId, storeObjectiveSwitchState ->
        ObjectiveViewState(items, selectedId, storeObjectiveSwitchState)
    }.asLiveData()

    fun onItemToggled(item: ObjectiveItem) {
        selectedId.update { item.id }
    }

    fun onBackPressed() {
        triggerEvent(Exit)
    }

    fun onSaveTapped() {
        selectedId.value?.let { triggerEvent(ExitWithResult(ObjectiveResult(it))) }
        // TODO analyticsTrackerWrapper.track(BLAZE_...)
    }

    data class ObjectiveViewState(
        val items: List<ObjectiveItem>,
        val selectedItemId: String? = null,
        val isStoreSelectionButtonToggled: Boolean = true,
    ) {
        val isSaveButtonEnabled: Boolean
            get() = !selectedItemId.isNullOrEmpty()
    }

    data class ObjectiveItem(
        val id: String,
        val title: String,
        val description: String,
        val suitableForDescription: String,
    )

    @Parcelize
    data class ObjectiveResult(val objectiveId: String) : Parcelable
}
