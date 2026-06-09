package com.woocommerce.android.ui.compose.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.woocommerce.android.ui.compose.designsystem.foundation.LocalWooColors
import com.woocommerce.android.ui.compose.designsystem.foundation.LocalWooPadding
import com.woocommerce.android.ui.compose.designsystem.foundation.LocalWooSpacing
import com.woocommerce.android.ui.compose.designsystem.foundation.LocalWooText
import com.woocommerce.android.ui.compose.designsystem.foundation.LocalWooTopAppBarAppearance
import com.woocommerce.android.ui.compose.designsystem.foundation.WooColors
import com.woocommerce.android.ui.compose.designsystem.foundation.WooPadding
import com.woocommerce.android.ui.compose.designsystem.foundation.WooSpacing
import com.woocommerce.android.ui.compose.designsystem.foundation.WooTypography

object WooTheme {
    val colors: WooColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWooColors.current

    val text: WooTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalWooText.current

    val spacing: WooSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalWooSpacing.current

    val padding: WooPadding
        @Composable
        @ReadOnlyComposable
        get() = LocalWooPadding.current

    internal val topAppBarAppearance: WooTopAppBarAppearance
        @Composable
        @ReadOnlyComposable
        get() = LocalWooTopAppBarAppearance.current
}
