package com.woocommerce.android.ui.woopos.common.composeui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
fun isPreviewMode(): Boolean {
    return LocalInspectionMode.current
}
