package com.woocommerce.android.ui.woopos.root

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WooPosActivity : AppCompatActivity() {
    @Inject
    lateinit var wooPosCardReaderFacade: WooPosCardReaderFacade

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        lifecycle.addObserver(wooPosCardReaderFacade)

        setContent {
            WooPosRootScreen(
                modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())
            )
        }
    }
}
