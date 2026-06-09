package com.woocommerce.android.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Creates a [ComposeView] with the [ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed] composition strategy,
 * using the default theme root selected by [FeatureFlag.NEW_DESIGN_SYSTEM].
 *
 * @param compositionStrategy To override the composition strategy.
 * @param content The content of the selected theme root.
 */
fun Fragment.composeView(
    compositionStrategy: ViewCompositionStrategy = ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    theme: ComposeTheme = defaultComposeTheme(),
    content: @Composable () -> Unit
) = ComposeView(requireContext()).apply {
    setViewCompositionStrategy(compositionStrategy)

    setContent {
        when (theme) {
            ComposeTheme.LEGACY -> WooThemeWithBackground(content = content)
            ComposeTheme.DESIGN_SYSTEM -> WooDesignSystemThemeWithBackground(content = content)
        }
    }
}

private fun Fragment.defaultComposeTheme(): ComposeTheme {
    val featureFlagRepository = EntryPoints.get(
        requireContext().applicationContext,
        ComposeViewFeatureFlagEntryPoint::class.java
    ).featureFlagRepository()

    return if (featureFlagRepository.isEnabled(FeatureFlag.NEW_DESIGN_SYSTEM)) {
        ComposeTheme.DESIGN_SYSTEM
    } else {
        ComposeTheme.LEGACY
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface ComposeViewFeatureFlagEntryPoint {
    fun featureFlagRepository(): FeatureFlagRepository
}

enum class ComposeTheme {
    LEGACY,
    DESIGN_SYSTEM,
}
