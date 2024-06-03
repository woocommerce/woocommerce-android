package com.woocommerce.android.ui.woopos.root

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import dagger.hilt.android.AndroidEntryPoint
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
            WooPosRootScreen()
        }
    }
}
