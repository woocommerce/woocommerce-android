package com.woocommerce.android.apifaker.ui.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot.Companion.withMutableSnapshot
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.Endpoint
import com.woocommerce.android.apifaker.models.EndpointType
import com.woocommerce.android.apifaker.models.EndpointWithResponse
import com.woocommerce.android.apifaker.models.FakeResponse
import com.woocommerce.android.apifaker.ui.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MISSING_ENDPOINT_ID = -1

@HiltViewModel
internal class EndpointDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val endpointDao: EndpointDao
) : ViewModel() {
    private val id = savedStateHandle.get<Int>(Screen.EndpointDetails.endpointIdArgumentName)!!

    var state: EndpointWithResponse by mutableStateOf(defaultEndpoint())
        private set

    init {
        if (id != MISSING_ENDPOINT_ID && state.endpoint.id == MISSING_ENDPOINT_ID) {
            loadEndpoint()
        }
    }

    fun onEndpointTypeChanged(endpointType: EndpointType) {
        withMutableSnapshot {
            state = state.copy(endpoint = state.endpoint.copy(type = endpointType))
        }
    }

    fun onRequestPathChanged(path: String) {
        withMutableSnapshot {
            state = state.copy(endpoint = state.endpoint.copy(path = path))
        }
    }

    fun onRequestBodyChanged(body: String) {
        withMutableSnapshot {
            state = state.copy(endpoint = state.endpoint.copy(body = body.ifEmpty { null }))
        }
    }

    fun onResponseStatusCodeChanged(statusCode: Int) {
        withMutableSnapshot {
            state = state.copy(response = state.response.copy(statusCode = statusCode))
        }
    }

    fun onResponseBodyChanged(body: String) {
        withMutableSnapshot {
            state = state.copy(response = state.response.copy(body = body))
        }
    }

    private fun loadEndpoint() = viewModelScope.launch {
        state = endpointDao.getEndpoint(id)!!
    }

    private fun defaultEndpoint() = EndpointWithResponse(
        Endpoint(
            id = MISSING_ENDPOINT_ID,
            type = EndpointType.WPApi,
            path = "",
            body = "%"
        ),
        FakeResponse(
            endpointId = MISSING_ENDPOINT_ID,
            statusCode = 200,
            body = ""
        )
    )
}
