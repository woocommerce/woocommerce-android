package com.woocommerce.android.ui.designsystem.compose

internal enum class WooTopAppBarAppearance {
    DesignSystem,

    /**
     * Temporary migration compatibility for `WooThemeWithBackground`.
     *
     * Remove this when legacy-compatible root rollout support is retired.
     */
    LegacyCompatible,
}
