package com.woocommerce.android.support.requests

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.support.TicketType
import com.woocommerce.android.support.TicketType.General
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class SupportRequestFormViewModel @Inject constructor(
    private val zendeskHelper: ZendeskHelper,
    private val selectedSite: SelectedSite,
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

    fun onHelpOptionSelected(helpOption: HelpOption) {
        viewState.update { it.copy(helpOption = helpOption) }
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
    @Parcelize
    data class ViewState(
        val helpOption: HelpOption?,
        val subject: String,
        val message: String
    ) : Parcelable {
        companion object {
            val EMPTY = ViewState(null, "", "")
        }
    }

    sealed class HelpOption(val ticketType: TicketType) : Parcelable {
        @Parcelize object MobileApp : HelpOption(General)
        @Parcelize object InPersonPayments : HelpOption(TicketType.Payments)
        @Parcelize object Payments : HelpOption(TicketType.Payments)
        @Parcelize object WooPlugin : HelpOption(General)
        @Parcelize object OtherPlugins : HelpOption(General)
    }
}
