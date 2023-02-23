package com.woocommerce.android.support.requests

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.support.TicketType
import com.woocommerce.android.support.TicketType.General
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.support.help.HelpOrigin
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
import zendesk.support.Request

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

    fun onSubmitRequestButtonClicked(context: Context, helpOrigin: HelpOrigin) {
        val helpOption = viewState.value.helpOption ?: return
        launch {
            zendeskHelper.createRequest(
                context,
                helpOrigin,
                selectedSite.get(),
                helpOption.ticketType,
                helpOption.extraTags,
                viewState.value.subject,
                viewState.value.message
            ).collect { it.handleCreateRequestResult() }
        }
    }

    private fun Result<Request?>.handleCreateRequestResult() {
        fold(
            onSuccess = { request ->

            },
            onFailure = {

            }
        )
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

    sealed class HelpOption(val ticketType: TicketType, val extraTags: List<String>) : Parcelable {
        @Parcelize object MobileApp : HelpOption(
            ticketType = General,
            extraTags = listOf("mobile-app")
        )
        @Parcelize object InPersonPayments : HelpOption(
            ticketType = General,
            extraTags = listOf("woocommerce_mobile_apps", "product_area_apps_in_person_payments")
        )
        @Parcelize object Payments : HelpOption(
            ticketType = TicketType.Payments,
            extraTags = emptyList()
        )
        @Parcelize object WooPlugin : HelpOption(
            ticketType = General,
            extraTags = listOf("woocommerce_core")
        )
        @Parcelize object OtherPlugins : HelpOption(
            ticketType = General,
            extraTags = listOf("product_area_woo_extensions")
        )
    }
}
