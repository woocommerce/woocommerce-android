package com.woocommerce.android.ui.sitepicker.sitediscovery

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.util.PatternsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin.LOGIN_SITE_ADDRESS
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import com.woocommerce.android.util.UrlUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.RegexOption.IGNORE_CASE

@HiltViewModel
class SitePickerSiteDiscoveryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sitePickRepository: SitePickerRepository,
    private val accountRepository: AccountRepository,
    private val resourceProvider: ResourceProvider,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val urlUtils: UrlUtils
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val FETCHED_URL_KEY = "fetched_url"
        private const val ADDRESS_VALIDATION_DEBOUNCE_DELAY_MS = 1000L
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
            Step.AddressInput -> prepareAddressViewState()
            Step.NotWordpress -> prepareNotWordpressSiteState()
        }
    }.asLiveData()

    private suspend fun FlowCollector<ViewState>.prepareAddressViewState() {
        val isLoadingFlow = MutableStateFlow(false)
        val isAddressSiteHelpShownFlow = MutableStateFlow(false)
        val addressValidationFlow = siteAddressFlow.map {
            PatternsCompat.WEB_URL.matcher(it).matches()
        }

        suspend fun emitViewState() {
            val viewStateFlow = combine(
                siteAddressFlow,
                addressValidationFlow,
                isLoadingFlow,
                isAddressSiteHelpShownFlow,
                inlineErrorFlow
            ) { address, isAddressValid, isLoading, displayLoadingDialog, error ->
                ViewState.AddressInputState(
                    siteAddress = address,
                    isAddressValid = isAddressValid,
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
            emitAll(viewStateFlow)
        }

        @OptIn(FlowPreview::class)
        suspend fun handleAddressValidationError() {
            addressValidationFlow.zip(siteAddressFlow) { isValid, address -> Pair(isValid, address) }
                .filter { (_, address) -> address.isNotEmpty() }
                .debounce(ADDRESS_VALIDATION_DEBOUNCE_DELAY_MS)
                .filterNot { (isValid, _) -> isValid }
                .collect { inlineErrorFlow.value = R.string.invalid_site_url_message }
        }

        coroutineScope {
            launch { emitViewState() }
            launch { handleAddressValidationError() }
        }
    }

    private suspend fun FlowCollector<ViewState>.prepareNotWordpressSiteState() {
        emit(
            ViewState.ErrorState(
                siteAddress = siteAddressFlow.value,
                message = resourceProvider.getString(R.string.login_not_wordpress_site_v2),
                imageResourceId = R.drawable.img_woo_generic_error,
                primaryButtonText = resourceProvider.getString(R.string.login_try_another_store),
                primaryButtonAction = {
                    stepFlow.value = Step.AddressInput
                },
                secondaryButtonText = resourceProvider.getString(R.string.login_try_another_account),
                secondaryButtonAction = ::logout
            )
        )
    }

    private suspend fun startSiteDiscovery() {
        // If the site is already connected to the account, go back to site picker
        if (sitePickRepository.getSiteBySiteUrl(urlUtils.sanitiseUrl(siteAddressFlow.value)) != null) {
            fetchedSiteUrl = siteAddressFlow.value
            navigateBackToSitePicker()
            return
        }

        sitePickRepository.fetchSiteInfo(siteAddressFlow.value).fold(
            onSuccess = {
                val siteAddress = urlUtils.sanitiseUrl(it.urlAfterRedirects ?: it.url)
                // Remove protocol prefix
                val protocolRegex = Regex("^(http[s]?://)", IGNORE_CASE)
                fetchedSiteUrl = siteAddress.replaceFirst(protocolRegex, "")

                analyticsTracker.track(
                    stat = AnalyticsEvent.SITE_PICKER_SITE_DISCOVERY,
                    properties = mapOf(
                        "user_entered_address" to siteAddressFlow.value,
                        "fetched_address" to siteAddress,
                        "has_wordpress" to it.isWordPress,
                        "is_wpcom" to it.isWPCom,
                        "is_jetpack_installed" to it.hasJetpack,
                        "is_jetpack_active" to it.isJetpackActive,
                        "is_jetpack_connected" to it.isJetpackConnected
                    )
                )

                when {
                    sitePickRepository.getSiteBySiteUrl(siteAddress) != null -> {
                        // If the site is already connected to the account, go back to site picker
                        // We need this additional handling here to handle any non-standard suffixes that the users
                        // might have added to the site address, and that weren't handled by [UrlUtils.sanitiseUrl],
                        // as the `urlAfterRedirects` coming from the API will clear those.
                        navigateBackToSitePicker()
                    }
                    !it.exists -> inlineErrorFlow.value = R.string.invalid_site_url_message
                    !it.isWordPress -> stepFlow.value = Step.NotWordpress
                    !it.isWPCom -> {
                        triggerEvent(
                            StartNativeJetpackActivation(
                                siteAddress = siteAddress,
                                isJetpackInstalled = it.isJetpackActive
                            )
                        )
                    }
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
        triggerEvent(NavigateToHelpScreen(LOGIN_SITE_ADDRESS))
    }

    fun onJetpackInstalled() {
        navigateBackToSitePicker()
    }

    fun onJetpackConnected() {
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
            accountRepository.logout().let {
                if (it) {
                    triggerEvent(Logout)
                }
            }
        }
    }

    private enum class Step {
        AddressInput, NotWordpress
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
    data class StartWebBasedJetpackInstallation(val siteAddress: String) : MultiLiveEvent.Event()
    data class StartNativeJetpackActivation(
        val siteAddress: String,
        val isJetpackInstalled: Boolean
    ) : MultiLiveEvent.Event()
}
