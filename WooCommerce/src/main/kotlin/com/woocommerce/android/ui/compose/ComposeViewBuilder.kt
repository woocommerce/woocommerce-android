package com.woocommerce.android.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

/**
 * Creates a [ComposeView] with the [ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed] composition strategy,
 * and the [WooThemeWithBackground] as the root composable.
 *
 * @param content The content of the [WooThemeWithBackground].
 */
fun Fragment.composeView(content: @Composable () -> Unit) = ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

    setContent {
        WooThemeWithBackground(content = content)
    }
}
