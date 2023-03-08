package com.woocommerce.android.apifaker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.apifaker.ApiFakerConfig
import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.Request
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val endpointDao: EndpointDao,
    private val config: ApiFakerConfig
) : ViewModel() {
    val endpoints = endpointDao.observeEndpoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isEnabled = config.enabled

    fun onMockingToggleChanged(enabled: Boolean) {
        viewModelScope.launch {
            config.setStatus(enabled)
        }
    }

    fun onRemoveRequest(request: Request) {
        viewModelScope.launch {
            endpointDao.deleteRequest(request)

            if (endpointDao.isEmpty()) {
                config.setStatus(false)
            }
        }
    }
}
