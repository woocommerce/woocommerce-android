package com.woocommerce.android.ui.woopos.cardreader

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
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
                triggerEvent(
                    WooPosCardReaderActivityEvent(
                        cardReaderFlowParam = CardReaderFlowParam.WooPosConnection,
                        cardReaderType = CardReaderType.EXTERNAL
                    )
                )
            }

            is WooPosCardReaderMode.Payment -> {
                if (mode.orderId != -1L) {
                    val orderId = savedStateHandle.get<Long>(WooPosCardReaderMode.Payment::orderId.name)!!
                    startPaymentFlow(orderId)
                } else {
                    launch {
                        createTestOrder(
                            onSuccess = { order ->
                                startPaymentFlow(order.id)
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

    private fun startPaymentFlow(orderId: Long) {
        triggerEvent(
            WooPosCardReaderActivityEvent(
                cardReaderFlowParam = CardReaderFlowParam.PaymentOrRefund.Payment(
                    orderId = orderId,
                    paymentType = CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.WOO_POS
                ),
                cardReaderType = CardReaderType.EXTERNAL
            )
        )
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
}
