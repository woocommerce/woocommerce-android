package com.woocommerce.android.ui.payments.cardreader.readermode

import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.woocommerce.android.ui.compose.theme.WooTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CardReaderModeActivity : AppCompatActivity() {

    private val viewModel: CardReaderModeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(FLAG_KEEP_SCREEN_ON)

        setContent {
            WooTheme {
                CardReaderModeScreen(viewModel = viewModel)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { finish() }
            }
        }
    }
}
