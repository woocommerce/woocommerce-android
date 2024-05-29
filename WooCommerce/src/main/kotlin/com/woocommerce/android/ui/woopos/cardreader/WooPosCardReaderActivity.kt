package com.woocommerce.android.ui.woopos.cardreader

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgument
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.tutorial.CardReaderTutorialDialogFragmentArgs
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@AndroidEntryPoint
class WooPosCardReaderActivity : AppCompatActivity(R.layout.activity_woo_pos_card_reader) {

    @Inject
    lateinit var orderCreateEditRepository: OrderCreateEditRepository

    @Inject
    lateinit var resourceProvider: ResourceProvider

    @Inject
    lateinit var cardReaderCountryConfigProvider: CardReaderCountryConfigProvider

    @Inject
    lateinit var wooStore: WooCommerceStore

    @Inject
    lateinit var selectedSite: SelectedSite

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.woopos_card_reader_nav_host_fragment) as NavHostFragment
        val countryConfig = cardReaderCountryConfigProvider.provideCountryConfigFor(
            wooStore.getStoreCountryCode(selectedSite.get())
        ) as CardReaderConfigForSupportedCountry

        val navController = navHostFragment.navController
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("WOOPOS-payment-flow", "current destination: $destination")
        }
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph_payment_flow).apply {
            setStartDestination(R.id.cardReaderStatusCheckerDialogFragment)
        }

        lifecycleScope.launch {
            val result = orderCreateEditRepository.createSimplePaymentOrder(
                countryConfig.minimumAllowedChargeAmount,
                customerNote = resourceProvider.getString(R.string.card_reader_tap_to_pay_test_payment_note),
                isTaxable = false,
            )
            result.fold(
                onSuccess = {
                    withContext(Dispatchers.Main) {
                        navController.setGraph(navGraph, buildPaymentArgs(it.id).toBundle())
                    }
                },
                onFailure = {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Failed to create order",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun buildPaymentArgs(orderId: Long) = CardReaderTutorialDialogFragmentArgs(
        CardReaderFlowParam.PaymentOrRefund.Payment(
            orderId = orderId,
            paymentType = CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.SIMPLE,
        ),
        CardReaderType.EXTERNAL
    )
}
