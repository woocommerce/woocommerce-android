package com.woocommerce.android.ui.blaze.creation.destination

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.util.getBaseUrl
import com.woocommerce.android.util.joinToString
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
            parameters = navArgs.url.parseParameters()
        )
    )

    val viewState = _viewState.asLiveData()

    fun onBackPressed() {
        triggerEvent(ExitWithResult(_viewState.value.url))
    }

    fun onAddParameterTapped() {
        /* TODO */
    }

    @Suppress("UNUSED_PARAMETER")
    fun onParameterTapped(key: String) {
        /* TODO */
    }

    fun onDeleteParameterTapped(key: String) {
        _viewState.update {
            it.copy(parameters = it.parameters - key)
        }
    }

    data class ViewState(
        private val baseUrl: String,
        val parameters: Map<String, String>
    ) {
        val url by lazy {
            parameters.joinToUrl(baseUrl)
        }

        val charactersRemaining: Int
            get() = MAX_CHARACTERS - parameters.joinToString().length
    }
}
