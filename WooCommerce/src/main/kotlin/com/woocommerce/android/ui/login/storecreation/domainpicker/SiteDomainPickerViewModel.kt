package com.woocommerce.android.ui.login.storecreation.domainpicker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SiteDomainPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = MutableLiveData<SiteDomainPickerState>()
    val viewState: LiveData<SiteDomainPickerState> = _viewState

    data class SiteDomainPickerState(
        val isLoading: Boolean = false
    )
}
