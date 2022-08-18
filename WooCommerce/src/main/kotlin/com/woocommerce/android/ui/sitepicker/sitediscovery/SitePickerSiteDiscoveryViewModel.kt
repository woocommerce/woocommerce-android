package com.woocommerce.android.ui.sitepicker.sitediscovery

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.util.PatternsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.ResourceProvider
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
import javax.inject.Inject
import kotlin.text.RegexOption.IGNORE_CASE

@HiltViewModel
class SitePickerSiteDiscoveryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sitePickRepository: SitePickerRepository,
    private val resourceProvider: ResourceProvider
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
    val viewState = stepFlow.transformLatest { step ->
        when (step) {
            Step.AddressInput -> emitAll(prepareAddressViewState())
            Step.JetpackUnavailable -> emit(prepareJetpackUnavailableState())
            Step.NotWordpress -> emit(prepareNotWordpressSiteState())
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

    private fun prepareJetpackUnavailableState() = ViewState.ErrorState(
        siteAddress = siteAddressFlow.value,
        message = resourceProvider.getString(R.string.login_jetpack_required_text, siteAddressFlow.value),
        imageResourceId = R.drawable.img_login_jetpack_required,
        primaryButtonText = resourceProvider.getString(R.string.login_jetpack_install),
        primaryButtonAction = {
            fetchedSiteUrl.let { url ->
                requireNotNull(url)
                triggerEvent(StartJetpackInstallation(url))
            }
        },
        secondaryButtonText = resourceProvider.getString(R.string.login_try_another_account),
        secondaryButtonAction = ::logout
    )

    private fun prepareNotWordpressSiteState() = ViewState.ErrorState(
        siteAddress = siteAddressFlow.value,
        message = resourceProvider.getString(R.string.login_not_wordpress_site_v2),
        imageResourceId = R.drawable.img_woo_no_stores,
        primaryButtonText = resourceProvider.getString(R.string.login_try_another_store),
        primaryButtonAction = {
            stepFlow.value = Step.AddressInput
        },
        secondaryButtonText = resourceProvider.getString(R.string.login_try_another_account),
        secondaryButtonAction = ::logout
    )

    private suspend fun startSiteDiscovery() {
        sitePickRepository.fetchSiteInfo(siteAddressFlow.value).fold(
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

    fun onJetpackInstalled() {
        navigateBackToSitePicker()
    }

    private fun navigateBackToSitePicker() {
        fetchedSiteUrl.let { url ->
            requireNotNull(url)
            triggerEvent(ExitWithResult(url))
        }
    }

    private fun logout() {
        launch {
            sitePickRepository.logout().let {
                if (it && sitePickRepository.isUserLoggedIn()) {
                    triggerEvent(Logout)
                }
            }
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
            @DrawableRes val imageResourceId: Int,
            val primaryButtonText: String,
            val primaryButtonAction: () -> Unit,
            val secondaryButtonText: String,
            val secondaryButtonAction: () -> Unit
        ) : ViewState()
    }

    object CreateZendeskTicket : MultiLiveEvent.Event()
    object NavigateToHelpScreen : MultiLiveEvent.Event()
    data class StartJetpackInstallation(val siteAddress: String) : MultiLiveEvent.Event()
}
