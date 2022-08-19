package com.woocommerce.android.ui.sitepicker.sitediscovery

import androidx.annotation.StringRes
import androidx.core.util.PatternsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import org.wordpress.android.login.R
import javax.inject.Inject
import kotlin.text.RegexOption.IGNORE_CASE

@HiltViewModel
class SitePickerSiteDiscoveryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchSiteInfo: FetchSiteInfo
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val FETCHED_URL_KEY = "fetched_url"
    }

    private val siteAddressFlow = savedStateHandle.getStateFlow(viewModelScope, "")
    private val stepFlow = savedStateHandle.getStateFlow(viewModelScope, Step.AddressInput)
    private val inlineErrorFlow = savedStateHandle.getStateFlow(viewModelScope, 0)

    private var fetchedSiteUrl
        get() = savedState.get<String>(FETCHED_URL_KEY)
        set(value) = savedState.set(FETCHED_URL_KEY, value)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewState = stepFlow.transformLatest<Step, ViewState> { step ->
        when (step) {
            Step.AddressInput -> emitAll(prepareAddressViewState())
            Step.JetpackUnavailable -> TODO()
            Step.NotWordpress -> TODO()
        }
    }.asLiveData()

    private fun prepareAddressViewState(): Flow<ViewState.AddressInputState> {
        val isLoadingFlow = MutableStateFlow(false)
        val isAddressSiteHelpShownFlow = MutableStateFlow(false)

        return combine(
            siteAddressFlow,
            isLoadingFlow,
            isAddressSiteHelpShownFlow,
            inlineErrorFlow
        ) { address, isLoading, displayLoadingDialog, error ->
            ViewState.AddressInputState(
                siteAddress = address,
                isAddressValid = PatternsCompat.WEB_URL.matcher(address).matches(),
                isLoading = isLoading,
                isAddressSiteHelpShown = displayLoadingDialog,
                inlineErrorMessage = error,
                onAddressChanged = {
                    inlineErrorFlow.value = 0
                    siteAddressFlow.value = it
                },
                onShowSiteAddressTapped = { isAddressSiteHelpShownFlow.value = true },
                onSiteAddressHelpDismissed = { isAddressSiteHelpShownFlow.value = false },
                onMoreHelpTapped = {
                    isAddressSiteHelpShownFlow.value = false
                    triggerEvent(CreateZendeskTicket)
                },
                onContinueTapped = {
                    launch {
                        isLoadingFlow.value = true
                        startSiteDiscovery()
                        isLoadingFlow.value = false
                    }
                }
            )
        }
    }

    private suspend fun startSiteDiscovery() {
        fetchSiteInfo(siteAddressFlow.value).fold(
            onSuccess = {
                val siteAddress = (it.urlAfterRedirects ?: it.url)
                // Remove protocol prefix
                val protocolRegex = Regex("^(http[s]?://)", IGNORE_CASE)
                fetchedSiteUrl = siteAddress.replaceFirst(protocolRegex, "")
                when {
                    !it.exists -> inlineErrorFlow.value = R.string.invalid_site_url_message
                    !it.isWordPress -> stepFlow.value = Step.NotWordpress
                    !it.isWPCom && (!it.hasJetpack || !it.isJetpackConnected) ->
                        stepFlow.value = Step.JetpackUnavailable
                    else -> navigateBackToSitePicker()
                }
            },
            onFailure = {
                inlineErrorFlow.value = R.string.invalid_site_url_message
            }
        )
    }

    fun onBackButtonClick() {
        triggerEvent(Exit)
    }

    fun onHelpButtonClick() {
        triggerEvent(NavigateToHelpScreen)
    }

    private fun navigateBackToSitePicker() {
        fetchedSiteUrl.let { url ->
            requireNotNull(url)
            triggerEvent(ExitWithResult(url))
        }
    }

    private enum class Step {
        AddressInput, JetpackUnavailable, NotWordpress
    }

    sealed class ViewState {
        abstract val siteAddress: String

        data class AddressInputState(
            override val siteAddress: String,
            val isAddressValid: Boolean,
            val isLoading: Boolean,
            val isAddressSiteHelpShown: Boolean,
            @StringRes val inlineErrorMessage: Int = 0,
            val onAddressChanged: (String) -> Unit,
            val onShowSiteAddressTapped: () -> Unit,
            val onSiteAddressHelpDismissed: () -> Unit,
            val onMoreHelpTapped: () -> Unit,
            val onContinueTapped: () -> Unit
        ) : ViewState()

        data class ErrorState(
            override val siteAddress: String,
            val message: String,
            val primaryButtonText: String,
            val primaryButtonAction: () -> Unit,
            val secondaryButtonText: String,
            val secondaryButtonAction: () -> Unit
        ) : ViewState()
    }

    object CreateZendeskTicket : MultiLiveEvent.Event()
    object NavigateToHelpScreen : MultiLiveEvent.Event()
}
