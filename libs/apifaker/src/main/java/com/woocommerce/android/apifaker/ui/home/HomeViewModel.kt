package com.woocommerce.android.apifaker.ui.home

import android.net.Uri
import android.util.Log
import androidx.compose.material.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.apifaker.ApiFakerConfig
import com.woocommerce.android.apifaker.EndpointExportManager
import com.woocommerce.android.apifaker.LOG_TAG
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
    private val config: ApiFakerConfig,
    private val endpointExportManager: EndpointExportManager,
    private val snackbarHostState: SnackbarHostState
) : ViewModel() {
    @Suppress("MagicNumber")
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
        }
    }

    fun onExportEndpoints(uri: Uri) {
        viewModelScope.launch {
            endpointExportManager.exportEndpoints(endpoints.value, uri).fold(
                onSuccess = {
                    snackbarHostState.showSnackbar("Endpoints exported successfully")
                },
                onFailure = {
                    snackbarHostState.showSnackbar("Failed to export endpoints")
                    Log.e(LOG_TAG, "Failed to export endpoints", it)
                }
            )
        }
    }

    fun onImportEndpoints(uri: Uri) {
        viewModelScope.launch {
            endpointExportManager.importEndpoints(uri).fold(
                onSuccess = {
                    snackbarHostState.showSnackbar("Endpoints imported successfully")
                },
                onFailure = {
                    snackbarHostState.showSnackbar(
                        "Failed to import endpoints, please ensure the file was exported from the same app version"
                    )
                    Log.e(LOG_TAG, "Failed to import endpoints", it)
                }
            )
        }
    }
}
