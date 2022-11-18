package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class StoreNamePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore
) : ScopedViewModel(savedStateHandle) {
    private val _storeName = savedState.getStateFlow(scope = this, initialValue = "")
    val storeName: LiveData<String> = _storeName.asLiveData()

    fun onCancelPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onStoreNameChanged(newName: String) {
        _storeName.value = newName
    }

    fun onContinueClicked() {
        newStore.store.update {
            it.copy(name = storeName.value)
        }
        triggerEvent(NavigateToNextStep)
    }

    object NavigateToNextStep : MultiLiveEvent.Event()
}
