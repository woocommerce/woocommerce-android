package com.woocommerce.android.ui.woopos.root

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.modifier.gesturesOrButtonsNavigationPadding
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
import com.woocommerce.android.ui.woopos.support.WooPosGetSupportFacade
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEntryPointKeeper
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

    @Inject
    lateinit var analyticsEntryPointKeeper: WooPosAnalyticsEntryPointKeeper

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

    override fun onPause() {
        super.onPause()
        endPosSessionIfFinishing()
    }

    override fun onDestroy() {
        endPosSessionIfFinishing()
        super.onDestroy()
    }

    private fun endPosSessionIfFinishing() {
        if (isFinishing) analyticsEntryPointKeeper.onPosSessionEnded()
    }

    companion object {
        private var hasInitializedSession = false
    }
}
