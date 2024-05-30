package com.woocommerce.android.ui.woopos.root

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
        setContent {
            MaterialTheme {
                WooPosRootHost(
                    connectToCardReader = {
                        lifecycleScope.launch {
                            wooPosCardReaderFacade.connectToReader(this@WooPosActivity)
                        }
                        lifecycleScope.launch {
                            wooPosCardReaderFacade.readerStatus.collect {
                                Toast.makeText(this@WooPosActivity, "Reader status: $it", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}
