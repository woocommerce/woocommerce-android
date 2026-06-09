package com.woocommerce.android.support.requests

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.zendesk.TicketType
import com.woocommerce.android.support.zendesk.ZendeskException.IdentityNotSetException
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.support.zendesk.ZendeskTicketRepository
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.aisupportchat.AiSupportChatAnalyticsTracker
import com.woocommerce.android.ui.aisupportchat.AiSupportChatTicketAnalyticsContext
import com.woocommerce.android.ui.aisupportchat.AiSupportChatTicketRoute
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import zendesk.support.Request
import javax.inject.Inject

@HiltViewModel
class SupportRequestFormViewModel @Inject constructor(
    private val zendeskTicketRepository: ZendeskTicketRepository,
    private val zendeskSettings: ZendeskSettings,
    private val selectedSite: SelectedSite,
    private val tracks: AnalyticsTrackerWrapper,
    private val aiSupportChatAnalyticsTracker: AiSupportChatAnalyticsTracker,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState.EMPTY
    )

    val isSubmitButtonEnabled = viewState
        .map { it.dataIsValid && it.isLoading.not() }
        .distinctUntilChanged()
        .asLiveData()

    val isRequestLoading = viewState
        .map { it.isLoading }
        .distinctUntilChanged()
        .asLiveData()

    fun onViewCreated() {
        tracks.track(AnalyticsEvent.SUPPORT_NEW_REQUEST_VIEWED)
    }

    fun onPrefillReceived(prefill: Prefill) {
        viewState.update {
            it.copy(
                ticketType = it.ticketType ?: prefill.ticketType,
                subject = it.subject.ifBlank { prefill.subject },
                siteAddress = it.siteAddress.ifBlank { prefill.siteAddress },
                message = it.message.ifBlank { prefill.message }
            )
        }
    }

    fun onHelpOptionSelected(ticketType: TicketType) {
        viewState.update { it.copy(ticketType = ticketType) }
    }

    fun onSubjectChanged(subject: String) {
        viewState.update { it.copy(subject = subject) }
    }

    fun onSiteAddressChanged(siteAddress: String) {
        viewState.update { it.copy(siteAddress = siteAddress) }
    }

    fun onMessageChanged(message: String) {
        viewState.update { it.copy(message = message) }
    }

    @Suppress("LongParameterList")
    fun onUserIdentitySet(
        context: Context,
        helpOrigin: HelpOrigin,
        extraTags: List<String>,
        diagnosticLog: String?,
        selectedEmail: String,
        selectedName: String,
        aiSupportChatTicketAnalyticsContext: AiSupportChatTicketAnalyticsContext? = null
    ) {
        zendeskSettings.supportEmail = selectedEmail
        zendeskSettings.supportName = selectedName
        tracks.track(AnalyticsEvent.SUPPORT_IDENTITY_SET)
        submitSupportRequest(
            context = context,
            helpOrigin = helpOrigin,
            extraTags = extraTags,
            diagnosticLog = diagnosticLog,
            aiSupportChatTicketAnalyticsContext = aiSupportChatTicketAnalyticsContext
        )
    }

    fun submitSupportRequest(
        context: Context,
        helpOrigin: HelpOrigin,
        extraTags: List<String>,
        diagnosticLog: String? = null,
        aiSupportChatTicketAnalyticsContext: AiSupportChatTicketAnalyticsContext? = null
    ) {
        val ticketType = viewState.value.ticketType ?: return

        viewState.update { it.copy(isLoading = true) }
        launch {
            zendeskTicketRepository.createRequest(
                context,
                helpOrigin,
                ticketType,
                selectedSite.getIfExists(),
                viewState.value.subject,
                viewState.value.message,
                extraTags,
                viewState.value.siteAddress,
                diagnosticLog
            ).collect { it.handleCreateRequestResult(aiSupportChatTicketAnalyticsContext) }
        }
    }

    private fun handleEmptyCredentials() {
        triggerEvent(
            ShowSupportIdentityInputDialog(
                emailSuggestion = zendeskSettings.supportEmail.orEmpty(),
                nameSuggestion = zendeskSettings.supportName.orEmpty()
            )
        )
    }

    private fun Result<Request?>.handleCreateRequestResult(
        aiSupportChatTicketAnalyticsContext: AiSupportChatTicketAnalyticsContext?
    ) {
        viewState.update { it.copy(isLoading = false) }
        fold(
            onSuccess = {
                triggerEvent(RequestCreationSucceeded)
                tracks.track(AnalyticsEvent.SUPPORT_NEW_REQUEST_CREATED)
                aiSupportChatTicketAnalyticsContext?.let {
                    aiSupportChatAnalyticsTracker.trackTicketCreated(
                        route = AiSupportChatTicketRoute.SUPPORT_FORM,
                        context = it
                    )
                }
            },
            onFailure = { error -> handleRequestCreationFailure(error, aiSupportChatTicketAnalyticsContext) }
        )
    }

    private fun handleRequestCreationFailure(
        error: Throwable,
        aiSupportChatTicketAnalyticsContext: AiSupportChatTicketAnalyticsContext?
    ) {
        aiSupportChatTicketAnalyticsContext?.let {
            WooLog.e(WooLog.T.AI, "Support chat ticket creation failed via support form", error)
        }
        tracks.track(AnalyticsEvent.SUPPORT_NEW_REQUEST_FAILED)
        aiSupportChatTicketAnalyticsContext?.let {
            aiSupportChatAnalyticsTracker.trackTicketCreationFailed(
                route = AiSupportChatTicketRoute.SUPPORT_FORM,
                context = it,
                error = error
            )
        }
        when (error) {
            is IdentityNotSetException -> handleEmptyCredentials()
            else -> triggerEvent(RequestCreationFailed)
        }
    }

    object RequestCreationSucceeded : Event()
    object RequestCreationFailed : Event()
    data class ShowSupportIdentityInputDialog(
        val emailSuggestion: String,
        val nameSuggestion: String
    ) : Event()

    data class Prefill(
        val ticketType: TicketType? = null,
        val subject: String = "",
        val siteAddress: String = "",
        val message: String = ""
    )

    @Parcelize
    data class ViewState(
        val ticketType: TicketType?,
        val subject: String,
        val siteAddress: String,
        val message: String,
        val isLoading: Boolean
    ) : Parcelable {
        val dataIsValid
            get() = ticketType != null && subject.isNotBlank() && siteAddress.isNotBlank() && message.isNotBlank()

        companion object {
            val EMPTY = ViewState(null, "", "", "", isLoading = false)
        }
    }
}
