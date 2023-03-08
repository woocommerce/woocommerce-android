package com.woocommerce.android.apifaker.ui.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot.Companion.withMutableSnapshot
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.Response
import com.woocommerce.android.apifaker.ui.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

const val MISSING_ENDPOINT_ID = 0L

@HiltViewModel
internal class EndpointDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val endpointDao: EndpointDao
) : ViewModel() {
    private val id = savedStateHandle.get<Long>(Screen.EndpointDetails.endpointIdArgumentName)!!

    var state: UiState by mutableStateOf(defaultEndpoint())
        private set

    init {
        if (id != MISSING_ENDPOINT_ID && state.request.id == MISSING_ENDPOINT_ID) {
            loadEndpoint()
        }
    }

    fun onApiTypeChanged(apiType: ApiType) {
        withMutableSnapshot {
            state = state.copy(request = state.request.copy(type = apiType))
        }
    }

    fun onRequestPathChanged(path: String) {
        withMutableSnapshot {
            state = state.copy(request = state.request.copy(path = path))
        }
    }

    fun onRequestBodyChanged(body: String) {
        withMutableSnapshot {
            state = state.copy(request = state.request.copy(body = body.ifEmpty { null }))
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

    fun onSaveClicked() {
        viewModelScope.launch {
            endpointDao.insertEndpoint(state.request, state.response)
            state = state.copy(isEndpointSaved = true)
        }
    }

    private fun loadEndpoint() = viewModelScope.launch {
        state = endpointDao.getEndpoint(id)!!.let {
            UiState(
                it.request,
                it.response
            )
        }
    }

    data class UiState(
        val request: Request,
        val response: Response,
        val isEndpointSaved: Boolean = false
    ) {
        val isEndpointValid: Boolean
            get() = request.path.isNotBlank()
    }

    private fun defaultEndpoint() = UiState(
        Request(
            id = MISSING_ENDPOINT_ID,
            type = ApiType.WPApi,
            path = "",
            body = null
        ),
        Response(
            endpointId = MISSING_ENDPOINT_ID,
            statusCode = 200,
            body = ""
        )
    )
}
