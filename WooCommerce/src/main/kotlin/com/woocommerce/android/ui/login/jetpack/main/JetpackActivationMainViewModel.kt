package com.woocommerce.android.ui.login.jetpack.main

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_SETUP_FLOW
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_JETPACK_SETUP_ACTIVATION_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_JETPACK_SETUP_INSTALL_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.support.help.HelpOrigin.JETPACK_INSTALLATION
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.common.PluginRepository
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginActivated
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginActivationFailed
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginInstallFailed
import com.woocommerce.android.ui.common.PluginRepository.PluginStatus.PluginInstalled
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.jetpack.GoToStore
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.ui.login.jetpack.connection.JetpackActivationWebViewViewModel
import com.woocommerce.android.ui.login.jetpack.connection.JetpackActivationWebViewViewModel.ConnectionResult.Cancel
import com.woocommerce.android.ui.login.jetpack.connection.JetpackActivationWebViewViewModel.ConnectionResult.Failure
import com.woocommerce.android.ui.login.jetpack.connection.JetpackActivationWebViewViewModel.ConnectionResult.Success
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore.JetpackConnectionUrlError
import org.wordpress.android.fluxc.store.JetpackStore.JetpackUserError
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

@HiltViewModel
class JetpackActivationMainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jetpackActivationRepository: JetpackActivationRepository,
    private val pluginRepository: PluginRepository,
    private val accountRepository: AccountRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val JETPACK_SLUG = "jetpack"
        private const val JETPACK_NAME = "jetpack/jetpack"
        private const val DELAY_AFTER_CONNECTION_MS = 500L
        private const val DELAY_BEFORE_SHOWING_ERROR_STATE_MS = 1000L
        private const val CONNECTED_EMAIL_KEY = "connected-email"

        @VisibleForTesting
        const val JETPACK_SITE_CONNECTED_AUTH_URL_PREFIX = "https://jetpack.wordpress.com/jetpack.authorize"

        @VisibleForTesting
        const val MOBILE_REDIRECT = "woocommerce://jetpack-connected"
    }

    private val navArgs: JetpackActivationMainFragmentArgs by savedStateHandle.navArgs()
    private val site: Deferred<SiteModel>
        get() = async {
            val site = jetpackActivationRepository.getSiteByUrl(navArgs.siteUrl)?.takeIf {
                val hasCredentials = it.username.isNotNullOrEmpty() && it.password.isNotNullOrEmpty()
                if (!hasCredentials) {
                    WooLog.w(WooLog.T.LOGIN, "The found site for jetpack activation doesn't have credentials")
                }
                hasCredentials
            }
            requireNotNull(site) {
                "Site not cached"
            }
        }
    private var jetpackConnectedEmail
        get() = savedState.get<String>(CONNECTED_EMAIL_KEY).orEmpty()
        set(value) = savedState.set(CONNECTED_EMAIL_KEY, value)

    private val currentStep = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = Step(if (navArgs.isJetpackInstalled) StepType.Connection else StepType.Installation),
    )
    private val connectionStep = savedStateHandle.getStateFlow(
        scope = viewModelScope,
        initialValue = ConnectionStep.PreConnection
    )
    private var isShowingErrorState = MutableStateFlow(false)
    val viewState = combine(
        currentStep,
        connectionStep,
        flowOf(if (navArgs.isJetpackInstalled) stepsForConnection() else stepsForInstallation()),
        isShowingErrorState
    ) { currentStep, connectionStep, stepTypes, isShowingErrorState ->
        when (isShowingErrorState) {
            false -> ViewState.ProgressViewState(
                siteUrl = UrlUtils.removeScheme(navArgs.siteUrl),
                isJetpackInstalled = navArgs.isJetpackInstalled,
                steps = stepTypes.map { stepType ->
                    Step(
                        type = stepType,
                        state = when {
                            currentStep.type == stepType -> currentStep.state
                            currentStep.type > stepType -> StepState.Success
                            else -> StepState.Idle
                        }
                    )
                },
                connectionStep = connectionStep
            )

            true -> ViewState.ErrorViewState(
                stepType = currentStep.type,
                errorCode = (currentStep.state as? StepState.Error)?.code
            )
        }
    }.asLiveData()

    private val isFromBanner = appPrefsWrapper.getJetpackInstallationIsFromBanner()

    init {
        if (!isFromBanner) {
            analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_SETUP_SCREEN_VIEWED)
        }

        monitorCurrentStep()
        handleErrorStates()
        startNextStep()
    }

    fun onCloseClick() {
        if (isFromBanner) {
            analyticsTrackerWrapper.track(
                stat = JETPACK_SETUP_FLOW,
                properties = mapOf(
                    AnalyticsTracker.KEY_STEP to currentStep.value.type.analyticsName,
                    AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_DISMISS
                )
            )
        } else {
            analyticsTrackerWrapper.track(
                stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_SCREEN_DISMISSED,
                properties = mapOf(
                    AnalyticsTracker.KEY_JETPACK_INSTALLATION_STEP to
                        currentStep.value.type.analyticsName
                )
            )
        }
        triggerEvent(Exit)
    }

    fun onContinueClick() = launch {
        if (isFromBanner) {
            analyticsTrackerWrapper.track(
                stat = JETPACK_SETUP_FLOW,
                properties = mapOf(
                    AnalyticsTracker.KEY_STEP to currentStep.value.type.analyticsName,
                    AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_JETPACK_SETUP_TAP_GO_TO_STORE
                )
            )
        } else {
            analyticsTrackerWrapper.track(stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_GO_TO_STORE_BUTTON_TAPPED)
        }

        val loggedInEmail = accountRepository.getUserAccount()?.email
        if (jetpackConnectedEmail == loggedInEmail) {
            val site = jetpackActivationRepository.getSiteByUrl(navArgs.siteUrl)
            requireNotNull(site) { "Illegal state, the button shouldn't be visible before fetching the site" }
            if (site.hasWooCommerce) {
                jetpackActivationRepository.setSelectedSiteAndCleanOldSites(site)
                triggerEvent(GoToStore)

                if (isFromBanner) {
                    analyticsTrackerWrapper.track(stat = AnalyticsEvent.JETPACK_SETUP_SYNCHRONIZATION_COMPLETED)
                }
            } else {
                triggerEvent(ShowWooNotInstalledScreen(navArgs.siteUrl))
            }
        } else {
            // Persist the site address to allow auto-login after password verification
            appPrefsWrapper.setLoginSiteAddress(navArgs.siteUrl)
            triggerEvent(GoToPasswordScreen(jetpackConnectedEmail))
        }
    }

    fun onJetpackConnectionResult(result: JetpackActivationWebViewViewModel.ConnectionResult) {
        when (result) {
            Success -> connectionStep.value = ConnectionStep.Validation
            Cancel -> triggerEvent(ShowWebViewDismissedError)
            is Failure -> currentStep.update {
                it.copy(
                    state = StepState.Error(result.errorCode)
                )
            }
        }
    }

    fun onRetryClick() {
        if (isFromBanner) {
            analyticsTrackerWrapper.track(
                stat = JETPACK_SETUP_FLOW,
                properties = mapOf(
                    AnalyticsTracker.KEY_STEP to currentStep.value.type.analyticsName,
                    AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_JETPACK_SETUP_TAP_TRY_AGAIN
                )
            )
        } else {
            analyticsTrackerWrapper.track(
                stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_TRY_AGAIN_BUTTON_TAPPED,
                properties = mapOf(
                    AnalyticsTracker.KEY_JETPACK_INSTALLATION_STEP to
                        currentStep.value.type.analyticsName
                )
            )
        }
        startNextStep()
    }

    fun onGetHelpClick() {
        if (isFromBanner) {
            analyticsTrackerWrapper.track(
                stat = JETPACK_SETUP_FLOW,
                properties = mapOf(
                    AnalyticsTracker.KEY_STEP to currentStep.value.type.analyticsName,
                    AnalyticsTracker.KEY_TAP to AnalyticsTracker.VALUE_JETPACK_SETUP_TAP_SUPPORT
                )
            )
        } else {
            analyticsTrackerWrapper.track(
                stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_GET_SUPPORT_BUTTON_TAPPED,
                properties = mapOf(
                    AnalyticsTracker.KEY_JETPACK_INSTALLATION_STEP to
                        currentStep.value.type.analyticsName
                ),
            )
        }
        triggerEvent(NavigateToHelpScreen(JETPACK_INSTALLATION))
    }

    private fun startNextStep() {
        currentStep.update { it.copy(state = StepState.Ongoing) }
    }

    private fun monitorCurrentStep() = launch {
        currentStep
            .map { step ->
                step.copy(
                    type = if (step.type == StepType.Activation) {
                        // To allow restarting the Jetpack installation after process-death, consider the Activation
                        // same as the Installation events
                        StepType.Installation
                    } else step.type
                )
            }
            .distinctUntilChanged()
            .collectLatest { step ->
                if (step.state != StepState.Ongoing) return@collectLatest

                val stepType = step.type
                WooLog.d(WooLog.T.LOGIN, "Jetpack Activation: handle step: $stepType")

                when (stepType) {
                    StepType.Installation -> {
                        startJetpackInstallation()
                    }

                    StepType.Connection -> {
                        connectionStep.collect { connectionStep ->
                            when (connectionStep) {
                                ConnectionStep.PreConnection -> startJetpackConnection()
                                ConnectionStep.Validation -> startJetpackValidation()
                                ConnectionStep.Approved -> withContext(NonCancellable) {
                                    currentStep.value = Step(
                                        type = StepType.Connection,
                                        state = StepState.Success
                                    )

                                    delay(DELAY_AFTER_CONNECTION_MS)
                                    currentStep.value = Step(
                                        type = StepType.Done,
                                        state = StepState.Ongoing
                                    )
                                }
                            }
                        }
                    }

                    StepType.Done -> {
                        if (isFromBanner) {
                            analyticsTrackerWrapper.track(stat = AnalyticsEvent.JETPACK_SETUP_COMPLETED)
                        } else {
                            analyticsTrackerWrapper.track(
                                stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_ALL_STEPS_MARKED_DONE
                            )
                        }

                        currentStep.value = Step(
                            type = StepType.Done,
                            state = StepState.Success
                        )
                    }

                    StepType.Activation -> error("Type Activation is not expected here")
                }
            }
    }

    private fun handleErrorStates() {
        currentStep.onEach { step ->
            when (step.state) {
                is StepState.Error -> {
                    delay(DELAY_BEFORE_SHOWING_ERROR_STATE_MS)
                    isShowingErrorState.value = true
                }

                else -> isShowingErrorState.value = false
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun startJetpackInstallation() {
        WooLog.d(WooLog.T.LOGIN, "Jetpack Activation: start Jetpack Installation")
        pluginRepository.installPlugin(
            site = site.await(),
            slug = JETPACK_SLUG,
            name = JETPACK_NAME
        ).collect { status ->
            when (status) {
                is PluginInstalled -> {
                    if (!isFromBanner) {
                        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_SETUP_INSTALL_SUCCESSFUL)
                    }
                    currentStep.value = Step(type = StepType.Activation, state = StepState.Ongoing)
                }

                is PluginInstallFailed -> {
                    trackPluginInstallationError(status)
                    currentStep.update { state -> state.copy(state = StepState.Error(status.errorCode)) }
                }

                is PluginActivated -> {
                    if (!isFromBanner) {
                        analyticsTrackerWrapper.track(AnalyticsEvent.LOGIN_JETPACK_SETUP_ACTIVATION_SUCCESSFUL)
                    }
                    currentStep.value = Step(type = StepType.Connection, state = StepState.Ongoing)
                }

                is PluginActivationFailed -> {
                    trackPluginActivationError(status)
                    currentStep.update { state -> state.copy(state = StepState.Error(status.errorCode)) }
                }
            }
        }
    }

    private fun trackPluginActivationError(status: PluginActivationFailed) {
        if (isFromBanner) {
            analyticsTrackerWrapper.track(
                stat = JETPACK_SETUP_FLOW,
                properties = mapOf(
                    AnalyticsTracker.KEY_STEP to currentStep.value.type.analyticsName,
                    AnalyticsTracker.KEY_FAILURE to "Jetpack activation failed: $status",
                )
            )
        } else {
            analyticsTrackerWrapper.track(
                stat = LOGIN_JETPACK_SETUP_ACTIVATION_FAILED,
                properties = mapOf(AnalyticsTracker.KEY_ERROR_CODE to status.errorCode.toString()),
                errorContext = this@JetpackActivationMainViewModel::class.simpleName,
                errorType = status.errorType,
                errorDescription = status.errorDescription
            )
        }
    }

    private fun trackPluginInstallationError(status: PluginInstallFailed) {
        if (isFromBanner) {
            analyticsTrackerWrapper.track(
                stat = JETPACK_SETUP_FLOW,
                properties = mapOf(
                    AnalyticsTracker.KEY_STEP to currentStep.value.type.analyticsName,
                    AnalyticsTracker.KEY_FAILURE to "Jetpack installation failed: $status",
                )
            )
        } else {
            analyticsTrackerWrapper.track(
                stat = LOGIN_JETPACK_SETUP_INSTALL_FAILED,
                properties = mapOf(AnalyticsTracker.KEY_ERROR_CODE to status.errorCode.toString()),
                errorContext = this@JetpackActivationMainViewModel::class.simpleName,
                errorType = status.errorType,
                errorDescription = status.errorDescription
            )
        }
    }

    @Suppress("LongMethod")
    private suspend fun startJetpackConnection() {
        WooLog.d(WooLog.T.LOGIN, "Jetpack Activation: start Jetpack Connection")
        val currentSite = site.await()
        val useApplicationPasswords = selectedSite.connectionType == SiteConnectionType.ApplicationPasswords
        jetpackActivationRepository.fetchJetpackConnectionUrl(currentSite, useApplicationPasswords).fold(
            onSuccess = { connectionUrl ->
                if (!isFromBanner) {
                    analyticsTrackerWrapper.track(
                        stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_FETCH_JETPACK_CONNECTION_URL_SUCCESSFUL
                    )
                }

                if (useApplicationPasswords) {
                    // Depending on the site's connection status, we should provide different URLs to the webview.
                    // If the site already has a Jetpack site-connection, we can use the API-given URL as-is. We
                    // know this is the case if the URL starts with JETPACK_SITE_CONNECTED_AUTH_URL_PREFIX.

                    // If the site lacks a connection, the API-provided URL will be in the format of
                    // https://{site_url}/wp-admin/admin.php?page=jetpack&action=register&_wpnonce={nonce}.
                    // For application password login, where we don't want to use cookie-nonce authentication, the URL
                    // above cannot be used to connect the site to Jetpack.
                    // See: https://github.com/woocommerce/woocommerce-android/issues/7525

                    // As a workaround, we use a special URL that enables site connection without the app needing
                    // cookie-nonce authentication. The format looks like below:
                    // https://wordpress.com/jetpack/connect?url=<site_url>
                    //  &mobile_redirect=woocommerce://jetpack-connected&from=mobile
                    // See: pe5sF9-1le-p2#comment-1942

                    val chosenUrl = if (connectionUrl.startsWith(JETPACK_SITE_CONNECTED_AUTH_URL_PREFIX)) {
                        connectionUrl
                    } else {
                        "https://wordpress.com/jetpack/connect?url=" + navArgs.siteUrl +
                            "&mobile_redirect=" + MOBILE_REDIRECT +
                            "&from=mobile"
                    }

                    triggerEvent(
                        ShowJetpackConnectionWebView(
                            url = chosenUrl
                        )
                    )
                } else {
                    triggerEvent(
                        ShowJetpackConnectionWebView(
                            url = connectionUrl
                        )
                    )
                }
            },
            onFailure = {
                val error = (it as? OnChangedException)?.error as? JetpackConnectionUrlError

                if (isFromBanner) {
                    analyticsTrackerWrapper.track(
                        stat = JETPACK_SETUP_FLOW,
                        properties = mapOf(
                            AnalyticsTracker.KEY_STEP to currentStep.value.type.analyticsName,
                            AnalyticsTracker.KEY_FAILURE to "Jetpack installation failed: ${it.message}",
                        )
                    )
                } else {
                    analyticsTrackerWrapper.track(
                        stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_FETCH_JETPACK_CONNECTION_URL_FAILED,
                        properties = mapOf(AnalyticsTracker.KEY_ERROR_CODE to error?.errorCode.toString()),
                        errorContext = this@JetpackActivationMainViewModel::class.simpleName,
                        errorType = it::class.simpleName,
                        errorDescription = it.message.orEmpty()
                    )
                }
                currentStep.update { state -> state.copy(state = StepState.Error(error?.errorCode)) }
            }
        )
    }

    private suspend fun startJetpackValidation() {
        WooLog.d(WooLog.T.LOGIN, "Jetpack Activation: start Jetpack Connection validation")
        jetpackActivationRepository.fetchJetpackConnectedEmail(site.await()).fold(
            onSuccess = { email ->
                jetpackConnectedEmail = email
                if (accountRepository.getUserAccount()?.email != email) {
                    if (!isFromBanner) {
                        analyticsTrackerWrapper.track(
                            stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_AUTHORIZED_USING_DIFFERENT_WPCOM_ACCOUNT
                        )
                    }
                    WooLog.d(
                        WooLog.T.LOGIN,
                        "Jetpack Activation: connection made using a different email than the logged in one"
                    )
                    connectionStep.value = ConnectionStep.Approved
                } else {
                    confirmSiteConnection()
                }
            },
            onFailure = {
                val error = (it as? OnChangedException)?.error as? JetpackUserError

                if (isFromBanner) {
                    analyticsTrackerWrapper.track(
                        stat = JETPACK_SETUP_FLOW,
                        properties = mapOf(
                            AnalyticsTracker.KEY_STEP to currentStep.value.type.analyticsName,
                            AnalyticsTracker.KEY_FAILURE to "Jetpack connection validation failed: ${it.message}",
                        )
                    )
                } else {
                    analyticsTrackerWrapper.track(
                        stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_ERROR_CHECKING_JETPACK_CONNECTION,
                        properties = mapOf(AnalyticsTracker.KEY_ERROR_CODE to error?.errorCode.toString()),
                        errorContext = this@JetpackActivationMainViewModel::class.simpleName,
                        errorType = it::class.simpleName,
                        errorDescription = it.message.orEmpty()
                    )
                }
                currentStep.update { state -> state.copy(state = StepState.Error(error?.errorCode)) }
                if (it is JetpackActivationRepository.JetpackMissingConnectionEmailException) {
                    // If we can't find a connected email, we can't confirm the site connection. Let's
                    // Go back to the connection step to try again.
                    connectionStep.value = ConnectionStep.PreConnection
                }
            }
        )
    }

    private suspend fun confirmSiteConnection() {
        WooLog.d(WooLog.T.LOGIN, "Jetpack Activation: fetch sites and confirm site connection")
        jetpackActivationRepository.fetchJetpackSite(navArgs.siteUrl).fold(
            onSuccess = {
                connectionStep.value = ConnectionStep.Approved
            },
            onFailure = {
                if (isFromBanner) {
                    analyticsTrackerWrapper.track(
                        stat = JETPACK_SETUP_FLOW,
                        properties = mapOf(
                            AnalyticsTracker.KEY_STEP to currentStep.value.type.analyticsName,
                            AnalyticsTracker.KEY_FAILURE to "Site connection confirmation failed: ${it.message}",
                        )
                    )
                } else {
                    analyticsTrackerWrapper.track(
                        stat = AnalyticsEvent.LOGIN_JETPACK_FETCHING_WPCOM_SITES_FAILED,
                        errorContext = this@JetpackActivationMainViewModel::class.simpleName,
                        errorType = it::class.simpleName,
                        errorDescription = it.message.orEmpty()
                    )
                }
                currentStep.update { state -> state.copy(state = StepState.Error(null)) }
            }
        )
    }

    private fun stepsForInstallation() = StepType.values()

    private fun stepsForConnection() = arrayOf(StepType.Connection, StepType.Done)

    sealed interface ViewState {
        data class ProgressViewState(
            val siteUrl: String,
            val isJetpackInstalled: Boolean,
            val steps: List<Step>,
            val connectionStep: ConnectionStep
        ) : ViewState {
            val isDone = steps.all { it.state == StepState.Success }
        }

        data class ErrorViewState(
            val stepType: StepType,
            val errorCode: Int?
        ) : ViewState
    }

    @Parcelize
    data class Step(
        val type: StepType,
        val state: StepState = StepState.Idle
    ) : Parcelable

    enum class StepType(val analyticsName: String) {
        // The declaration order is important
        Installation("installation"),
        Activation("activation"),
        Connection("connection"),
        Done("all_done")
    }

    sealed interface StepState : Parcelable {
        @Parcelize
        object Idle : StepState

        @Parcelize
        object Ongoing : StepState

        @Parcelize
        object Success : StepState

        @Parcelize
        data class Error(val code: Int?) : StepState
    }

    enum class ConnectionStep {
        PreConnection, Validation, Approved
    }

    data class ShowJetpackConnectionWebView(
        val url: String
    ) : MultiLiveEvent.Event()

    data class GoToPasswordScreen(val email: String) : MultiLiveEvent.Event()
    data class ShowWooNotInstalledScreen(val siteUrl: String) : MultiLiveEvent.Event()
    object ShowWebViewDismissedError : MultiLiveEvent.Event()
}
