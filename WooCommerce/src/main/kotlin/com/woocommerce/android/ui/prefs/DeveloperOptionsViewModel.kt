package com.woocommerce.android.ui.prefs

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.ToggleableListItem
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val developerOptionsRepository: DeveloperOptionsRepository
) : ScopedViewModel(savedState) {

    private val _viewState = MutableLiveData(
        DeveloperOptionsViewState(
            rows = (
                createDeveloperOptionsList()
                )
        )
    )

    val viewState: LiveData<DeveloperOptionsViewState> = _viewState

    private fun createDeveloperOptionsList(): List<ListItem> = mutableListOf(
        ToggleableListItem(
            icon = drawable.img_card_reader_connecting,
            label = UiStringRes(string.enable_card_reader),
            devOptionsKey = UiStringRes(string.simulated_reader_key),
            isEnabled = true,
            isChecked = developerOptionsRepository.isSimulatedCardReaderEnabled(),
            onToggled = ::onSimulatedReaderToggled
        )
    )


    private fun onSimulatedReaderToggled(isChecked: Boolean) {
        if (!isChecked) {
//            launch {
//                developerOptionsRepository.clearSelectedCardReader()
//            }
            developerOptionsRepository.showToast()
        }

        developerOptionsRepository.changeSimulatedReaderState(isChecked)
        val currentViewState = viewState.value
        (currentViewState?.rows?.find {
            it.devOptionsKey == UiStringRes(string.simulated_reader_key)
        } as? ToggleableListItem)?.let { originalListItem ->
            val newState = originalListItem.copy(isChecked = isChecked)
            _viewState.value = currentViewState.copy(
                rows = currentViewState.rows.map {
                    if (it.label == newState.label)
                        newState
                    else it
                }
            )
        }
    }

    data class DeveloperOptionsViewState(
        val rows: List<ListItem>
    ) {
        sealed class ListItem {
            abstract val label: UiString
            abstract val icon: Int?
            abstract var isEnabled: Boolean
            abstract var devOptionsKey: UiString

            data class ToggleableListItem(
                @DrawableRes override val icon: Int,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override var devOptionsKey: UiString,
                val onToggled: (Boolean) -> Unit,
                val isChecked: Boolean
            ) : ListItem()

            data class NonToggleableListItem(
                @DrawableRes override val icon: Int,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override var devOptionsKey: UiString,
                val onClick: () -> Unit
            ) : ListItem()
        }
    }
}
