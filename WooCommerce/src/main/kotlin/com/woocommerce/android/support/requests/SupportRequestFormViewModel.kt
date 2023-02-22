package com.woocommerce.android.support.requests

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.support.TicketType
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

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
        .map { it.helpOption != HelpOption.None && it.subject.isNotBlank() && it.message.isNotBlank() }
        .distinctUntilChanged()
        .asLiveData()

    fun onHelpOptionSelected(helpDescription: String) {
        viewState.update { it.copy(helpOption = HelpOption.fromDescription(helpDescription)) }
    }

    fun onSubmitRequestButtonClicked(
        context: Context,
        ticketType: TicketType,
        subject: String,
        message: String
    ) {
        launch { zendeskHelper.createRequest(context, selectedSite.get(), ticketType, subject, message) }
    }

    data class ViewState(
        val helpOption: HelpOption,
        val subject: String,
        val message: String
    ) {
        companion object {
            val EMPTY = ViewState(HelpOption.None, "", "")
        }
    }

    sealed class HelpOption(val ticketType: TicketType?) {
        object MobileApp: HelpOption(TicketType.General)
        object InPersonPayments: HelpOption(TicketType.Payments)
        object Payments: HelpOption(TicketType.Payments)
        object WooPlugin: HelpOption(TicketType.General)
        object OtherPlugins: HelpOption(TicketType.General)
        object None: HelpOption(null)

        companion object {
            fun fromDescription(description: String): HelpOption {
                return when (description) {
                    "Mobile App" -> MobileApp
                    "In-Person Payments" -> InPersonPayments
                    "Payments" -> Payments
                    "WooCommerce Plugin" -> WooPlugin
                    "Other Plugins" -> OtherPlugins
                    else -> None
                }
            }
        }
    }
}
