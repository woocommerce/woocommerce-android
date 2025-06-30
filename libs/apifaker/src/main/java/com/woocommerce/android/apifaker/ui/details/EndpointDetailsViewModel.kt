package com.woocommerce.android.apifaker.ui.details

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.apifaker.AutoCompleteProvider
import com.woocommerce.android.apifaker.AutoCompleteSuggestion
import com.woocommerce.android.apifaker.db.EndpointDao
import com.woocommerce.android.apifaker.models.ApiType
import com.woocommerce.android.apifaker.models.HttpMethod
import com.woocommerce.android.apifaker.models.QueryParameter
import com.woocommerce.android.apifaker.models.Request
import com.woocommerce.android.apifaker.models.Response
import com.woocommerce.android.apifaker.ui.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

const val MISSING_ENDPOINT_ID = 0L

@HiltViewModel
internal class EndpointDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val endpointDao: EndpointDao,
    private val autoCompleteProvider: AutoCompleteProvider,
    private val snackbarHostState: SnackbarHostState
) : ViewModel() {
    private val id = checkNotNull(savedStateHandle.get<Long>(Screen.EndpointDetails.endpointIdArgumentName))

    var state: UiState by mutableStateOf(defaultEndpoint())
        private set

    var autoCompleteSuggestions by mutableStateOf(emptyList<AutoCompleteSuggestion>())
        private set

    init {
        viewModelScope.launch {
            if (id != MISSING_ENDPOINT_ID && state.request.id == MISSING_ENDPOINT_ID) {
                loadEndpoint()
            }

            handleAutoCompleteSuggestions()
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun handleAutoCompleteSuggestions() {
        snapshotFlow { state.request.path }
            .drop(1)
            .debounce(300.milliseconds)
            .map { path ->
                if (path.length > 2) {
                    autoCompleteProvider.provideAutoCompleteSuggestions(state.request.type, path)
                } else {
                    emptyList()
                }
            }
            .collect { autoCompleteSuggestions = it }
    }

    fun onApiTypeChanged(apiType: ApiType) {
        state = state.copy(request = state.request.copy(type = apiType))
    }

    fun onRequestHttpMethodChanged(httpMethod: HttpMethod?) {
        state = state.copy(request = state.request.copy(httpMethod = httpMethod))
    }

    fun onRequestPathChanged(path: String) {
        state = state.copy(request = state.request.copy(path = path))
    }

    fun onSuggestionSelected(suggestion: AutoCompleteSuggestion) {
        state = state.copy(request = state.request.copy(path = suggestion.endpoint))

        if (!suggestion.isNameSpaceConfirmed) {
            viewModelScope.launch {
                snackbarHostState.showSnackbar("Please verify and confirm that the suggested namespace is correct")
            }
        }
    }

    fun onQueryParameterAdded(name: String, value: String) {
        val queryParameter = QueryParameter(name, value)
        state = state.copy(
            request = state.request.copy(
                queryParameters = state.request.queryParameters + queryParameter
            )
        )
    }

    fun onQueryParameterDeleted(queryParameter: QueryParameter) {
        state = state.copy(
            request = state.request.copy(
                queryParameters = state.request.queryParameters - queryParameter
            )
        )
    }

    fun onRequestBodyChanged(body: String) {
        state = state.copy(request = state.request.copy(body = body.ifEmpty { null }))
    }

    fun onResponseStatusCodeChanged(statusCode: Int) {
        state = state.copy(response = state.response.copy(statusCode = statusCode))
    }

    fun onResponseBodyChanged(body: String) {
        state = state.copy(response = state.response.copy(body = body))
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            endpointDao.insertEndpoint(state.request, state.response)
            state = state.copy(isEndpointSaved = true)
        }
    }

    private suspend fun loadEndpoint() {
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
            httpMethod = null,
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
