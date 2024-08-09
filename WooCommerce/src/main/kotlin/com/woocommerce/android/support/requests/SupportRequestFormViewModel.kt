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

    fun onUserIdentitySet(
        context: Context,
        helpOrigin: HelpOrigin,
        extraTags: List<String>,
        selectedEmail: String,
        selectedName: String
    ) {
        zendeskSettings.supportEmail = selectedEmail
        zendeskSettings.supportName = selectedName
        tracks.track(AnalyticsEvent.SUPPORT_IDENTITY_SET)
        submitSupportRequest(context = context, helpOrigin = helpOrigin, extraTags = extraTags)
    }

    fun submitSupportRequest(
        context: Context,
        helpOrigin: HelpOrigin,
        extraTags: List<String>
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
                viewState.value.siteAddress
            ).collect { it.handleCreateRequestResult() }
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

    private fun Result<Request?>.handleCreateRequestResult() {
        viewState.update { it.copy(isLoading = false) }
        fold(
            onSuccess = {
                triggerEvent(RequestCreationSucceeded)
                tracks.track(AnalyticsEvent.SUPPORT_NEW_REQUEST_CREATED)
            },
            onFailure = ::handleRequestCreationFailure
        )
    }

    private fun handleRequestCreationFailure(error: Throwable) {
        tracks.track(AnalyticsEvent.SUPPORT_NEW_REQUEST_FAILED)
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
