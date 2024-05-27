package com.woocommerce.android.ui.woopos.cardreader

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.moremenu.MoreMenuFragmentDirections
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooPosCardReaderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_woo_pos_card_reader)

        val navController = findNavController(R.id.woopos_card_reader_nav_host_fragment)
        navController.setGraph(R.navigation.nav_graph_payment_flow, intent.extras)
        navController.navigate(
            MoreMenuFragmentDirections.actionMoreMenuToPaymentFlow(
                CardReaderFlowParam.PaymentOrRefund.Payment(
                    orderId = 1,
                    paymentType = CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.SIMPLE,
                )
            )
        )
    }
}
