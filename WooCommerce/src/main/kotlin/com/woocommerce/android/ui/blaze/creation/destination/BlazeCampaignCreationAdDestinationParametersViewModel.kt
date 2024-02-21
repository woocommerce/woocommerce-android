package com.woocommerce.android.ui.blaze.creation.destination

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.BlazeRepository.DestinationParameters
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState.ParameterBottomSheetState.Editing
import com.woocommerce.android.ui.blaze.creation.destination.BlazeCampaignCreationAdDestinationParametersViewModel.ViewState.ParameterBottomSheetState.Hidden
import com.woocommerce.android.util.joinToUrl
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationAdDestinationParametersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val MAX_CHARACTERS_FOR_DESTINATION_URL = 2096
    }

    private val navArgs: BlazeCampaignCreationAdDestinationParametersFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState(
            targetUrl = navArgs.destinationParameters.targetUrl,
            parameters = navArgs.destinationParameters.parameters,
            bottomSheetState = Hidden
        )
    )

    val viewState = _viewState.asLiveData()

    fun onBackPressed() {
        triggerEvent(ExitWithResult(DestinationParameters(_viewState.value.targetUrl, _viewState.value.parameters)))
    }

    fun onAddParameterTapped() {
        _viewState.update {
            it.copy(
                bottomSheetState = Editing(
                    targetUrl = it.targetUrl,
                    parameters = it.parameters
                )
            )
        }
    }

    fun onParameterTapped(key: String) {
        _viewState.update {
            it.copy(
                bottomSheetState = Editing(
                    targetUrl = it.targetUrl,
                    parameters = it.parameters - key,
                    key = key,
                    value = it.parameters[key] ?: ""
                )
            )
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
            it.copy(
                bottomSheetState = (it.bottomSheetState as Editing).copy(
                    key = key,
                    value = value,
                    error = getError(key, value)
                )
            )
        }
    }

    private fun getError(key: String, value: String): Int {
        val editingState = _viewState.value.bottomSheetState as Editing
        val parametersLength = (editingState.parameters + (key to value)).entries.joinToString("&").length
        return when {
            editingState.parameters.containsKey(key) -> {
                R.string.blaze_campaign_edit_ad_destination_key_exists_error
            }

            parametersLength >= MAX_CHARACTERS_FOR_DESTINATION_URL -> {
                R.string.blaze_campaign_edit_ad_destination_too_long_error
            }

            else -> 0
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

    @Parcelize
    data class ViewState(
        val targetUrl: String,
        val parameters: Map<String, String>,
        val bottomSheetState: ParameterBottomSheetState
    ) : Parcelable {
        @IgnoredOnParcel
        val url by lazy {
            parameters.joinToUrl(targetUrl)
        }

        val charactersRemaining: Int
            get() = MAX_CHARACTERS_FOR_DESTINATION_URL - parameters.entries.joinToString("&").length

        sealed interface ParameterBottomSheetState : Parcelable {
            @Parcelize
            data object Hidden : ParameterBottomSheetState

            @Parcelize
            data class Editing(
                val targetUrl: String,
                val parameters: Map<String, String>,
                val key: String = "",
                val value: String = "",
                @StringRes val error: Int = 0
            ) : ParameterBottomSheetState {
                @IgnoredOnParcel
                val url: String by lazy {
                    if (key.isNotEmpty()) {
                        parameters + (key to value)
                    } else {
                        parameters
                    }.joinToUrl(targetUrl)
                }

                val isSaveButtonEnabled: Boolean
                    get() = key.isNotEmpty() && value.isNotEmpty() && error == 0
            }
        }
    }
}
