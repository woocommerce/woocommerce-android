package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize

@Composable
fun Modifier.wooPosErrorAndEmptyStateButton(): Modifier = this
    .fillMaxWidth(0.5f)
    .height(WooPosComponentSize.Small.value)
