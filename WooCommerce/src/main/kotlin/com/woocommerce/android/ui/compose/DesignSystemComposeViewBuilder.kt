package com.woocommerce.android.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground

/**
 * Creates a [ComposeView] with the Store design-system theme root.
 */
fun Fragment.designSystemComposeView(
    compositionStrategy: ViewCompositionStrategy = ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    content: @Composable () -> Unit
) = ComposeView(requireContext()).apply {
    setViewCompositionStrategy(compositionStrategy)

    setContent {
        WooDesignSystemThemeWithBackground(content = content)
    }
}
