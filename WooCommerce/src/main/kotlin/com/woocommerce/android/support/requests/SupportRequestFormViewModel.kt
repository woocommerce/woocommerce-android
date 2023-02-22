package com.woocommerce.android.support.requests

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.support.TicketType
import com.woocommerce.android.support.TicketType.General
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.HelpOption.InPersonPayments
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.HelpOption.MobileApp
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.HelpOption.OtherPlugins
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.HelpOption.Payments
import com.woocommerce.android.support.requests.SupportRequestFormViewModel.HelpOption.WooPlugin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportRequestFormViewModel @Inject constructor(
    private val zendeskHelper: ZendeskHelper,
    private val selectedSite: SelectedSite,
    private val resourceProvider: ResourceProvider,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState.EMPTY
    )

    val isSubmitButtonEnabled = viewState
        .map { it.helpOption != null && it.subject.isNotBlank() && it.message.isNotBlank() }
        .distinctUntilChanged()
        .asLiveData()

    fun onHelpOptionSelected(helpDescription: String) {
        viewState.update { it.copy(helpOption = generateHelpOptionFromDescription(helpDescription)) }
    }

    fun onSubjectChanged(subject: String) {
        viewState.update { it.copy(subject = subject) }
    }

    fun onMessageChanged(message: String) {
        viewState.update { it.copy(message = message) }
    }

    fun onSubmitRequestButtonClicked(context: Context) {
        val ticketType = viewState.value.helpOption?.ticketType ?: return
        val subject = viewState.value.subject
        val message = viewState.value.message
        launch { zendeskHelper.createRequest(context, selectedSite.get(), ticketType, subject, message) }
    }

    private fun generateHelpOptionFromDescription(description: String): HelpOption {
        return when (description) {
            resourceProvider.getString(MobileApp.descriptionResource) -> MobileApp
            resourceProvider.getString(InPersonPayments.descriptionResource) -> InPersonPayments
            resourceProvider.getString(Payments.descriptionResource) -> Payments
            resourceProvider.getString(WooPlugin.descriptionResource) -> WooPlugin
            resourceProvider.getString(OtherPlugins.descriptionResource) -> OtherPlugins
            else -> throw IllegalArgumentException("Unknown help option: $description")
        }
    }

    data class ViewState(
        val helpOption: HelpOption?,
        val subject: String,
        val message: String
    ) {
        companion object {
            val EMPTY = ViewState(null, "", "")
        }
    }

    sealed class HelpOption(val ticketType: TicketType, val descriptionResource: Int) {
        object MobileApp : HelpOption(General, R.string.support_request_help_app)
        object InPersonPayments : HelpOption(TicketType.Payments, R.string.support_request_help_ipp)
        object Payments : HelpOption(TicketType.Payments, R.string.support_request_help_payments)
        object WooPlugin : HelpOption(General, R.string.support_request_help_plugins)
        object OtherPlugins : HelpOption(General, R.string.support_request_help_other)
    }
}
