package com.woocommerce.android.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.designsystem.DesignSystemMode
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.designsystem.defaultDesignSystemMode

/**
 * Creates a [ComposeView] with the [ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed] composition strategy,
 * using the default theme root selected by [DesignSystemMode].
 *
 * @param compositionStrategy To override the composition strategy.
 * @param content The content of the selected theme root.
 */
fun Fragment.composeView(
    compositionStrategy: ViewCompositionStrategy = ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    mode: DesignSystemMode = defaultDesignSystemMode(),
    content: @Composable () -> Unit
) = configureComposeView(
    composeView = ComposeView(requireContext()),
    compositionStrategy = compositionStrategy,
    mode = mode,
    content = content,
)

/**
 * Configures an existing XML-hosted [ComposeView] with the selected design-system root.
 *
 * Use this for small Compose islands inside retained XML/View screens.
 */
fun Fragment.configureComposeView(
    composeView: ComposeView,
    compositionStrategy: ViewCompositionStrategy = ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    mode: DesignSystemMode = defaultDesignSystemMode(),
    content: @Composable () -> Unit
) = composeView.apply {
    setViewCompositionStrategy(compositionStrategy)

    setContent {
        when (mode.toComposeRoot()) {
            ComposeRoot.LEGACY -> WooThemeWithBackground(content = content)
            ComposeRoot.DESIGN_SYSTEM -> WooDesignSystemThemeWithBackground(content = content)
        }
    }
}

internal fun DesignSystemMode.toComposeRoot(): ComposeRoot =
    when (this) {
        DesignSystemMode.LEGACY -> ComposeRoot.LEGACY
        DesignSystemMode.DESIGN_SYSTEM -> ComposeRoot.DESIGN_SYSTEM
    }

internal enum class ComposeRoot {
    LEGACY,
    DESIGN_SYSTEM,
}
