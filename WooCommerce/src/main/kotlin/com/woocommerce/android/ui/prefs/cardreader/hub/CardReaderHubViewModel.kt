package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

class CardReaderHubViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState =
        MutableLiveData<CardReaderHubViewState>()
    val viewStateData: LiveData<CardReaderHubViewState> = viewState

    sealed class CardReaderHubViewState
}
