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
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val viewState = MutableLiveData(
        DeveloperOptionsViewState(
            rows = (
                createDeveloperOptionsList()
                ).sortedBy { it.index }
        )
    )

    val viewStateData: LiveData<DeveloperOptionsViewState> = viewState

    private fun createDeveloperOptionsList(): List<ListItem> = mutableListOf(
        ToggleableListItem(
            icon = drawable.img_card_reader_connecting,
            label = UiStringRes(string.enable_card_reader),
            isEnabled = false,
            index = 0,
            isChecked = false,
            onClick = {},
            onToggled = {}
        )
    )

    data class DeveloperOptionsViewState(
        val rows: List<ListItem>
    ) {
        sealed class ListItem {
            abstract val label: UiString
            abstract val icon: Int?
            abstract val onClick: (() -> Unit)?
            abstract val index: Int
            abstract var isEnabled: Boolean

            data class ToggleableListItem(
                @DrawableRes override val icon: Int,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override val index: Int,
                override val onClick: () -> Unit,
                val onToggled: (Boolean) -> Unit,
                val isChecked: Boolean
            ) : ListItem()

            data class NonToggleableListItem(
                @DrawableRes override val icon: Int,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override val index: Int,
                override val onClick: () -> Unit
            ) : ListItem()
        }
    }
}
