package com.woocommerce.android.ui.woopos.root

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.support.WooPosGetSupportFacade
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WooPosActivity : AppCompatActivity() {
    @Inject
    lateinit var wooPosCardReaderFacade: WooPosCardReaderFacade

    @Inject
    lateinit var wooPosGetSupportFacade: WooPosGetSupportFacade

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        lifecycle.addObserver(wooPosCardReaderFacade)
        lifecycle.addObserver(wooPosGetSupportFacade)

        val composeView = ComposeView(this).apply {
            id = com.woocommerce.android.R.id.pos_mode_compose_view
        }

        setContentView(composeView)

        composeView.setContent {
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
    val isGestureNavigation = insets.isGestureNavigation(view.context)

    return if (isGestureNavigation) {
        this.padding(bottom = 0.dp)
    } else {
        this.navigationBarsPadding()
    }
}

// That seems to be different on different devices, but 32dp is a common upper value
private const val GESTURE_NAVIGATION_BAR_HEIGHT_DP = 32
private fun WindowInsetsCompat.isGestureNavigation(context: Context): Boolean {
    val bottomInset = getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

    val gestureNavigationBarHeightPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        GESTURE_NAVIGATION_BAR_HEIGHT_DP.toFloat(),
        context.resources.displayMetrics
    ).toInt()

    return bottomInset in 1..gestureNavigationBarHeightPx
}
