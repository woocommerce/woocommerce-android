package com.woocommerce.android.ui.woopos.cardreader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.woocommerce.android.R
import com.woocommerce.android.ui.payments.cardreader.statuschecker.CardReaderStatusCheckerDialogFragmentArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooPosCardReaderActivity : AppCompatActivity(R.layout.activity_woo_pos_card_reader) {
    val viewModel: WooPosCardReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.woopos_card_reader_nav_host_fragment
        ) as NavHostFragment

        observeEvents(navHostFragment)
    }

    private fun observeEvents(navHostFragment: NavHostFragment) {
        viewModel.event.observe(this) { event ->
            when (event) {
                is StartCardReaderConnectionFlow -> {
                    val navController = navHostFragment.navController
                    val graph = navController.navInflater.inflate(R.navigation.nav_graph_payment_flow).apply {
                        setStartDestination(R.id.cardReaderStatusCheckerDialogFragment)
                    }
                    navController.setGraph(
                        graph,
                        CardReaderStatusCheckerDialogFragmentArgs(
                            cardReaderFlowParam = event.cardReaderFlowParam,
                            cardReaderType = event.cardReaderType,
                        ).toBundle()
                    )
                }
            }
        }
    }

    companion object {
        internal const val WOO_POS_CARD_READER_MODE_KEY = "card_reader_connection_mode"

        fun buildIntentForCardReaderConnection(context: Context) =
            Intent(context, WooPosCardReaderActivity::class.java).apply {
                putExtra(WOO_POS_CARD_READER_MODE_KEY, WooPosCardReaderMode.Connection)
            }
    }
}
