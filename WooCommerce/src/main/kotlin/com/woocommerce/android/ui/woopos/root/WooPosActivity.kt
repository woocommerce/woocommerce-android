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
        // all live in in-memory singletons. On process death these are lost, but Compose
        // Navigation restores the back stack to the home screen, skipping the splash flow
        // that initializes them — causing IllegalStateException in WooPosProductsDataSource.
        // Passing null forces a fresh start from the splash screen, which re-initializes
        // everything. On config changes the process stays alive and singletons survive,
        // so going through splash again is instant (no loading screen).
        super.onCreate(null)
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
