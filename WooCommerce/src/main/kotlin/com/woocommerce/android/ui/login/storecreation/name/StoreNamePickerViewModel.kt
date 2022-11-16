package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class StoreNamePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore
) : ScopedViewModel(savedStateHandle) {
    private val _storeName = MutableLiveData("")
    val storeName: LiveData<String> = _storeName

    fun onCancelPressed() {}

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
