package com.woocommerce.android.ui.woopos.root

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.root.navigation.WooPosRootHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WooPosActivity : AppCompatActivity() {
    @Inject
    lateinit var wooPosCardReaderFacade: WooPosCardReaderFacade

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        lifecycle.addObserver(wooPosCardReaderFacade)

        setContent {
            MaterialTheme {
                WooPosRootHost(
                    connectToCardReader = {
                        lifecycleScope.launch {
                            wooPosCardReaderFacade.connectToReader()
                        }
                        lifecycleScope.launch {
                            wooPosCardReaderFacade.readerStatus.collect {
                                Toast.makeText(this@WooPosActivity, "Reader status: $it", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    collectPaymentWithCardReader = {
                        lifecycleScope.launch {
                            val result = wooPosCardReaderFacade.collectPayment(-1)
                            Toast.makeText(this@WooPosActivity, "Payment result: $result", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
