package com.woocommerce.android.ui.readermode

import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.payments.cardreader.payment.remote.ExitCardReaderMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardReaderModeActivity : AppCompatActivity() {

    @Inject
    lateinit var session: CardReaderRemoteSession

    @Inject
    lateinit var stateBinder: CardReaderModeStateBinder

    private val viewModel: CardReaderModeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(FLAG_KEEP_SCREEN_ON)

        setContent {
            WooTheme {
                CardReaderModeScreen(viewModel = viewModel)
            }
        }

        viewModel.events.observe(this) { event ->
            if (event is ExitCardReaderMode) {
                event.isHandled = true
                finish()
            }
        }

        stateBinder.bind(lifecycleScope)
        session.start(lifecycleScope)
    }

    override fun onDestroy() {
        session.stop()
        super.onDestroy()
    }
}
