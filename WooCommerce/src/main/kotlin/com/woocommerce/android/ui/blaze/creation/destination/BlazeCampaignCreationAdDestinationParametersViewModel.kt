package com.woocommerce.android.ui.blaze.creation.destination

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState.ParameterBottomSheetState.Editing
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState.ParameterBottomSheetState.Hidden
import com.woocommerce.android.util.getBaseUrl
import com.woocommerce.android.util.joinToUrl
import com.woocommerce.android.util.parseParameters
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationAdDestinationParametersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    companion object {
        // The maximum number of characters allowed in a URL by Chrome
        private const val MAX_CHARACTERS = 2096
    }
    private val navArgs: BlazeCampaignCreationAdDestinationParametersFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = MutableStateFlow(
        ViewState(
            baseUrl = navArgs.url.getBaseUrl(),
            parameters = navArgs.url.parseParameters(),
            bottomSheetState = Hidden
        )
    )

    val viewState = _viewState.asLiveData()

    fun onBackPressed() {
        triggerEvent(ExitWithResult(_viewState.value.url))
    }

    fun onAddParameterTapped() {
        _viewState.update {
            it.copy(bottomSheetState = Editing(
                baseUrl = it.baseUrl,
                parameters = it.parameters
            ))
        }
    }

    fun onParameterTapped(key: String) {
        _viewState.update {
            it.copy(bottomSheetState = Editing(
                baseUrl = it.baseUrl,
                parameters = it.parameters - key,
                key = key,
                value = it.parameters[key] ?: ""
            ))
        }
    }

    fun onDeleteParameterTapped(key: String) {
        _viewState.update {
            it.copy(parameters = it.parameters - key)
        }
    }

    fun onParameterBottomSheetDismissed() {
        _viewState.update {
            it.copy(bottomSheetState = Hidden)
        }
    }

    fun onParameterChanged(key: String, value: String) {
        _viewState.update {
            it.copy(bottomSheetState = (it.bottomSheetState as Editing).copy(key = key, value = value))
        }
    }

    fun onParameterSaved(key: String, value: String) {
        _viewState.update {
            val params = it.parameters.toMutableMap()
            params[key] = value
            it.copy(
                parameters = params,
                bottomSheetState = Hidden
            )
        }
    }

    data class ViewState(
        val baseUrl: String,
        val parameters: Map<String, String>,
        val bottomSheetState: ParameterBottomSheetState
    ) {
        val url by lazy {
            parameters.joinToUrl(baseUrl)
        }

        val charactersRemaining: Int
            get() = MAX_CHARACTERS - parameters.entries.joinToString("&").length

        sealed interface ParameterBottomSheetState {
            data object Hidden : ParameterBottomSheetState
            data class Editing(
                val baseUrl: String,
                val parameters: Map<String, String>,
                val key: String = "",
                val value: String = ""
            ) : ParameterBottomSheetState {
                val url by lazy {
                    if (key.isNotEmpty()) {
                        parameters + (key to value)
                    } else {
                        parameters
                    }.joinToUrl(baseUrl)
                }
            }
        }
    }
}
