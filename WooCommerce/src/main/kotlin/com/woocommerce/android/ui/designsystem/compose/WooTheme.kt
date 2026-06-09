package com.woocommerce.android.ui.designsystem.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.woocommerce.android.ui.designsystem.compose.foundation.LocalWooColors
import com.woocommerce.android.ui.designsystem.compose.foundation.LocalWooPadding
import com.woocommerce.android.ui.designsystem.compose.foundation.LocalWooSpacing
import com.woocommerce.android.ui.designsystem.compose.foundation.LocalWooText
import com.woocommerce.android.ui.designsystem.compose.foundation.LocalWooTopAppBarAppearance
import com.woocommerce.android.ui.designsystem.compose.foundation.WooColors
import com.woocommerce.android.ui.designsystem.compose.foundation.WooPadding
import com.woocommerce.android.ui.designsystem.compose.foundation.WooSpacing
import com.woocommerce.android.ui.designsystem.compose.foundation.WooTypography

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
