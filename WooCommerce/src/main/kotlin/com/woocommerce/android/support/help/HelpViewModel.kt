package com.woocommerce.android.support.help

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class HelpViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) : ScopedViewModel(savedState) {
    fun onContactPaymentsSupportClicked() {
        triggerEvent(ContactPaymentsSupportClickEvent.Loading)
        launch {
            wooStore.fetchSitePlugins(selectedSite.get())
        }
    }

    sealed class ContactPaymentsSupportClickEvent : MultiLiveEvent.Event() {
        object Loading : ContactPaymentsSupportClickEvent()
        data class CreateTicket(val supportTags: List<String>) : ContactPaymentsSupportClickEvent()
    }
}
