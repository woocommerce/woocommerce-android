package com.woocommerce.android.support.requests

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.support.TicketType
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupportRequestFormViewModel @Inject constructor(
    private val zendeskHelper: ZendeskHelper,
    private val selectedSite: SelectedSite,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun onSubmitRequestButtonClicked(
        context: Context,
        ticketType: TicketType,
        subject: String,
        message: String
    ) {
        launch { zendeskHelper.createRequest(context, selectedSite.get(), ticketType, subject, message) }
    }

    sealed class HelpOption(val ticketType: TicketType) {
        object MobileApp: HelpOption(TicketType.General)
        object InPersonPayments: HelpOption(TicketType.Payments)
        object Payments: HelpOption(TicketType.Payments)
        object WooPlugin: HelpOption(TicketType.General)
        object OtherPlugins: HelpOption(TicketType.General)
    }
}
