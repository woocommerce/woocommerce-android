package com.woocommerce.android.ui.woopos.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosHomeViewModel @Inject constructor() : ViewModel() {
    init {
        viewModelScope.launch {
        }
    }
}
