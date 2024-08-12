package com.woocommerce.android.ui.woopos.root

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
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
            WooPosTheme {
                SystemBars()

                WooPosRootScreen(
                    modifier = Modifier.gesturesOrButtonsNavigationPadding()
                )
            }
        }
    }

    @Composable
    private fun SystemBars() {
        SideEffect {
            window.statusBarColor = getColor(android.R.color.transparent)
            window.navigationBarColor = getColor(android.R.color.transparent)
        }
    }
}

@Composable
private fun Modifier.gesturesOrButtonsNavigationPadding(): Modifier {
    val view = LocalView.current
    val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets)
    val isGestureNavigation = insets.isGestureNavigation()

    return if (isGestureNavigation) {
        this.padding(bottom = 0.dp)
    } else {
        this.navigationBarsPadding()
    }
}

private fun WindowInsetsCompat.isGestureNavigation(): Boolean {
    val bottomInset = getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

    // That seems to be different on different devices, but 48dp is a common upper value
    val gestureNavigationBarHeight = 48
    return bottomInset <= gestureNavigationBarHeight
}

