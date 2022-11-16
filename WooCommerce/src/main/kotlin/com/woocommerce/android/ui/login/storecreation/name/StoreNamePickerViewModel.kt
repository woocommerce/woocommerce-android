package com.woocommerce.android.ui.login.storecreation.name

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StoreNamePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val _storeName = MutableLiveData("")
    val storeName: LiveData<String> = _storeName

    fun onCancelPressed() {}

    fun onStoreNameChanged(newName: String) {
        _storeName.value = newName
    }

    fun onContinueClicked() {

    }
}
