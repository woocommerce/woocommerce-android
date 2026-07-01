package com.woocommerce.android.ui.woopos.common.composeui.modifier

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsCompat
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.util.ext.isGestureNavigation

/**
 * Adds bottom padding so content sits above the system navigation bar with 3-button navigation,
 * and no extra padding with gesture navigation.
 *
 * POS draws edge-to-edge and renders some steps in separate Dialog windows that don't inherit the
 * host Activity's insets, so this must be applied per-window to keep the bottom CTA in a consistent
 * position across the whole flow.
 */
@Composable
fun Modifier.gesturesOrButtonsNavigationPadding(): Modifier {
    val view = LocalView.current
    val insets = WindowInsetsCompat.toWindowInsetsCompat(view.rootWindowInsets)
    val isGestureNavigation = insets.isGestureNavigation(view.context)

    return if (isGestureNavigation) {
        this.padding(bottom = WooPosSpacing.None.value)
    } else {
        this.navigationBarsPadding()
    }
}
