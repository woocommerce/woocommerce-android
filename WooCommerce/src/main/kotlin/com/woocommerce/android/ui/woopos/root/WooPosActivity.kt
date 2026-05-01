package com.woocommerce.android.ui.woopos.root

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
import com.woocommerce.android.ui.woopos.support.WooPosGetSupportFacade
import com.woocommerce.android.ui.woopos.util.ext.isGestureNavigation
import com.woocommerce.android.ui.woopos.util.ext.lockWooPosOrientation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WooPosActivity : AppCompatActivity() {
    @Inject
    lateinit var wooPosGetSupportFacade: WooPosGetSupportFacade

    @Inject
    lateinit var wooPosCouponCreationFacade: WooPosCouponCreationFacade

    @Inject
    lateinit var wooPosPeriodicSyncFacade: WooPosPeriodicSyncFacade

    override fun onCreate(savedInstanceState: Bundle?) {
        // POS is session-based: cart, product cache, order cache, and data source selection
        // all live in in-memory singletons set up by the splash flow. On a config change
        // (theme, rotation, font scale) the process stays alive and the singletons survive,
        // so we preserve savedInstanceState and let Compose Navigation restore the back
        // stack to the screen the user was on. On process death the OS spawns a fresh
        // process with empty singletons; restoring the back stack to a non-splash route
        // would skip initialization and crash in WooPosProductsDataSource — so we discard
        // savedInstanceState to force a fresh splash. hasInitializedSession is process-
        // scoped: it's reset to false in a new process, true after the first onCreate.
        super.onCreate(if (hasInitializedSession) savedInstanceState else null)
        hasInitializedSession = true
        WindowCompat.setDecorFitsSystemWindows(window, false)
        lockWooPosOrientation()

        lifecycle.addObserver(wooPosGetSupportFacade)
        lifecycle.addObserver(wooPosCouponCreationFacade)
        lifecycle.addObserver(wooPosPeriodicSyncFacade)

        setContent {
            WooPosTheme {
                WooPosRootScreen(modifier = Modifier.gesturesOrButtonsNavigationPadding())
            }
        }
    }

    companion object {
        private var hasInitializedSession = false
    }
}

@Composable
private fun Modifier.gesturesOrButtonsNavigationPadding(): Modifier {
    val view = LocalView.current
    val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets)
    val isGestureNavigation = insets.isGestureNavigation(view.context)

    return if (isGestureNavigation) {
        this.padding(bottom = WooPosSpacing.None.value)
    } else {
        this.navigationBarsPadding()
    }
}
