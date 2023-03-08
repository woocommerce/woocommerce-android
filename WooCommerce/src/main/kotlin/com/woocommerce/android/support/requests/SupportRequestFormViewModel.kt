package com.woocommerce.android.support.requests

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.support.TicketType
import com.woocommerce.android.support.ZendeskManager
import com.woocommerce.android.support.ZendeskSettings
import com.woocommerce.android.support.help.HelpOrigin
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
    private val zendeskManager: ZendeskManager,
    private val zendeskSettings: ZendeskSettings,
    private val selectedSite: SelectedSite,
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

    fun onHelpOptionSelected(ticketType: TicketType) {
        viewState.update { it.copy(ticketType = ticketType) }
    }

    fun onSubjectChanged(subject: String) {
        viewState.update { it.copy(subject = subject) }
    }

    fun onMessageChanged(message: String) {
        viewState.update { it.copy(message = message) }
    }

    fun onUserIdentitySet(
        context: Context,
        helpOrigin: HelpOrigin,
        extraTags: List<String>,
        selectedEmail: String
    ) {
        zendeskSettings.supportEmail = selectedEmail
        AnalyticsTracker.track(AnalyticsEvent.SUPPORT_IDENTITY_SET)
        onSubmitRequestButtonClicked(
            context = context,
            helpOrigin = helpOrigin,
            extraTags = extraTags
        )
    }

    fun onSubmitRequestButtonClicked(
        context: Context,
        helpOrigin: HelpOrigin,
        extraTags: List<String>,
        verifyIdentity: Boolean = false
    ) {
        val ticketType = viewState.value.ticketType ?: return
        if (verifyIdentity && AppPrefs.hasSupportEmail().not()) {
            handleEmptyCredentials()
            return
        }

        viewState.update { it.copy(isLoading = true) }
        launch {
            zendeskManager.createRequest(
                context,
                helpOrigin,
                ticketType,
                selectedSite.getIfExists(),
                viewState.value.subject,
                viewState.value.message,
                extraTags
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
            onSuccess = { triggerEvent(RequestCreationSucceeded) },
            onFailure = { triggerEvent(RequestCreationFailed) }
        )
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
        val message: String,
        val isLoading: Boolean
    ) : Parcelable {
        val dataIsValid
            get() = ticketType != null && subject.isNotBlank() && message.isNotBlank()

        companion object {
            val EMPTY = ViewState(null, "", "", false)
        }
    }
}
