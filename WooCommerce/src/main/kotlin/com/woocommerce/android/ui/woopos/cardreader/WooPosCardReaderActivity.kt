package com.woocommerce.android.ui.woopos.cardreader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.woocommerce.android.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooPosCardReaderActivity : AppCompatActivity(R.layout.activity_woo_pos_card_reader) {
    val viewModel: WooPosCardReaderActivityViewModel by viewModels()

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
                    navController.navInflater.inflate(R.navigation.nav_graph_card_reader_connection_flow)
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
