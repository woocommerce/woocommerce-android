package com.woocommerce.android.apifaker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.apifaker.ApiFakerConfig
import com.woocommerce.android.apifaker.db.EndpointDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    endpointDao: EndpointDao,
    private val config: ApiFakerConfig
) : ViewModel() {
    val endpoints = endpointDao.observeEndpoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isEnabled = config.enabled

    fun onMockingToggleChanged(enabled: Boolean) {
        config.setStatus(enabled)
    }
}
