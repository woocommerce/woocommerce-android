package com.woocommerce.android.ui.woopos.cardreader

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderActivity.Companion.WOO_POS_CARD_READER_MODE_KEY
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class WooPosCardReaderViewModel @Inject constructor(
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val resourceProvider: ResourceProvider,
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    init {
        when (val mode = savedStateHandle.get<WooPosCardReaderMode>(WOO_POS_CARD_READER_MODE_KEY)) {
            is WooPosCardReaderMode.Connection -> {
                triggerEvent(WooPosCardReaderEvent.Connection)
            }

            is WooPosCardReaderMode.Payment -> {
                if (mode.orderId != -1L) {
                    triggerEvent(WooPosCardReaderEvent.Payment(mode.orderId))
                } else {
                    launch {
                        createTestOrder(
                            onSuccess = { order ->
                                triggerEvent(WooPosCardReaderEvent.Payment(order.id))
                            },
                            onFailure = {
                                Log.e("WooPosCardReaderViewModel", "Failed to create test order")
                            }
                        )
                    }
                }
            }

            null -> error("WooPosCardReaderMode not found in savedStateHandle")
        }
    }

    private suspend fun createTestOrder(onSuccess: (Order) -> Unit, onFailure: () -> Unit) {
        val countryConfig = cardReaderCountryConfigProvider.provideCountryConfigFor(
            wooStore.getStoreCountryCode(selectedSite.get())
        ) as CardReaderConfigForSupportedCountry

        val result = orderCreateEditRepository.createSimplePaymentOrder(
            countryConfig.minimumAllowedChargeAmount,
            customerNote = resourceProvider.getString(R.string.card_reader_tap_to_pay_test_payment_note),
            isTaxable = false,
        )
        result.fold(
            onSuccess = { onSuccess(it) },
            onFailure = { onFailure() }
        )
    }

    fun onPaymentResult(result: WooPosCardReaderPaymentResult) {
        Log.e("WooPosCardReaderViewModel", "Payment result: $result")
    }
}
