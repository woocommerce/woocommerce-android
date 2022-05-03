package com.woocommerce.android.ui.cardreader.manuals

import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderManualsViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

    private val _manualState = getManualItems().toMutableStateList()
    val manualState: List<ManualItem>
        get() = _manualState

    private fun getManualItems(): List<ManualItem> = listOf(
        ManualItem(
            icon = R.drawable.ic_card_reader_manual,
            label = R.string.card_reader_bbpos_manual_card_reader,
            onManualClicked = { }
        ),
        ManualItem(
            icon = R.drawable.ic_card_reader_manual,
            label = R.string.card_reader_m2_manual_card_reader,
            onManualClicked = { }
        )
    )

    data class ManualItem(
        val icon: Int,
        val label: Int,
        val onManualClicked: () -> Unit
    )
}
