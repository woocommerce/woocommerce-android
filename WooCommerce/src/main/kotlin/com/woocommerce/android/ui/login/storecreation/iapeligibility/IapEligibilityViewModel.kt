package com.woocommerce.android.ui.login.storecreation.iapeligibility

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IapEligibilityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,

    ) : ScopedViewModel(savedStateHandle) {
    private val _iapEligibleState =
        savedState.getStateFlow(scope = this, initialValue = IapEligibilityState(isLoading = true))
    val iapEligibleState: LiveData<IapEligibilityState> = _iapEligibleState.asLiveData()

    init {
        launch {

        }
    }

    data class IapEligibilityState(
        val isLoading: Boolean = false,
        val isEligible: Boolean = false
    )
}
