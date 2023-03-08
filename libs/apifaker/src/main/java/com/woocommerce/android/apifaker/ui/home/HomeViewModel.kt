package com.woocommerce.android.apifaker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.apifaker.db.EndpointDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    endpointDao: EndpointDao
) : ViewModel() {
    val endpoints = endpointDao.observeEndpoints()
        .map { list -> list.map { it.request } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onMockingToggleChanged(enabled: Boolean) {
        TODO()
    }
}
