package com.woocommerce.android.ui.woopos.cardreader

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.woocommerce.android.R
import com.woocommerce.android.extensions.parcelable
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentViewModel
import com.woocommerce.android.ui.payments.cardreader.statuschecker.CardReaderStatusCheckerDialogFragmentArgs
import com.woocommerce.android.ui.payments.methodselection.SelectPaymentMethodFragmentArgs
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WooPosCardReaderActivity : AppCompatActivity(R.layout.activity_woo_pos_card_reader) {
    val viewModel: WooPosCardReaderViewModel by viewModels()
    private val cardReaderPaymentViewModel: CardReaderPaymentViewModel by viewModels()
    @Inject
    lateinit var observer: IppFlowObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE


        // TODO: insert [CardReaderPaymentDialogFragmentArgs] to bundle/savedStateHandle so that it can be used in [CardReaderPaymentViewModel].
        // TODO: start with [SelectPaymentMethodViewModel] and observe their events and state, also propagating state to [IppFlowObserver].
        cardReaderPaymentViewModel.onViewCreated()
        cardReaderPaymentViewModel.start()
        cardReaderPaymentViewModel.viewStateData.observe(this) { viewState ->
            observer.notify(viewState, cardReaderPaymentViewModel)
        }
    }

    private fun observeResult(navHostFragment: NavHostFragment) {
        navHostFragment.childFragmentManager.setFragmentResultListener(
            WOO_POS_CARD_PAYMENT_REQUEST_KEY,
            this
        ) { requestKey, bundle ->
            when (requestKey) {
                WOO_POS_CARD_PAYMENT_REQUEST_KEY -> {
                    val result = bundle.parcelable<WooPosCardReaderPaymentStatus>(
                        WOO_POS_CARD_PAYMENT_RESULT_KEY
                    )
                    setResult(
                        RESULT_OK,
                        Intent().apply { putExtra(WOO_POS_CARD_PAYMENT_RESULT_KEY, result) }
                    )
                    finish()
                }

                else -> logResultListenerError(requestKey)
            }
        }

        navHostFragment.childFragmentManager.setFragmentResultListener(
            WOO_POS_CARD_CONNECTION_REQUEST_KEY,
            this
        ) { requestKey, _ ->
            when (requestKey) {
                WOO_POS_CARD_CONNECTION_REQUEST_KEY -> {
                    finish()
                }

                else -> logResultListenerError(requestKey)
            }
        }
    }

    private fun setupNavGraph(navHostFragment: NavHostFragment) {
        when (val mode = viewModel.cardReaderMode) {
            is WooPosCardReaderMode.Payment -> {
                val navController = navHostFragment.navController
                val graph = navController.navInflater.inflate(R.navigation.nav_graph_payment_flow)
                navController.setGraph(
                    graph,
                    SelectPaymentMethodFragmentArgs(cardReaderFlowParam = mode.cardReaderFlowParam).toBundle()
                )
            }

            WooPosCardReaderMode.Connection -> {
                val navController = navHostFragment.navController
                val graph =
                    navController.navInflater.inflate(R.navigation.nav_graph_payment_flow).apply {
                        setStartDestination(R.id.cardReaderStatusCheckerDialogFragment)
                    }
                navController.setGraph(
                    graph,
                    CardReaderStatusCheckerDialogFragmentArgs(
                        cardReaderFlowParam = mode.cardReaderFlowParam,
                        cardReaderType = mode.cardReaderType,
                    ).toBundle()
                )
            }
        }
    }

    private fun logResultListenerError(requestKey: String) {
        val errorMessage = "Unknown request key: $requestKey"
        WooLog.e(WooLog.T.POS, "Error in WooPosCardReaderActivity - $errorMessage")
        error(errorMessage)
    }

    companion object {
        const val WOO_POS_CARD_PAYMENT_REQUEST_KEY = "woo_pos_card_payment_request"
        const val WOO_POS_CARD_CONNECTION_REQUEST_KEY = "woo_pos_card_connection_request"
        const val WOO_POS_CARD_PAYMENT_RESULT_KEY = "woo_pos_card_payment_result"
        internal const val WOO_POS_CARD_READER_MODE_KEY = "card_reader_connection_mode"

        fun buildIntentForCardReaderConnection(context: Context) =
            Intent(context, WooPosCardReaderActivity::class.java).apply {
                putExtra(WOO_POS_CARD_READER_MODE_KEY, WooPosCardReaderMode.Connection)
            }

        fun buildIntentForPayment(context: Context, orderId: Long) =
            Intent(context, WooPosCardReaderActivity::class.java).apply {
                putExtra(WOO_POS_CARD_READER_MODE_KEY, WooPosCardReaderMode.Payment(orderId))
            }
    }
}
