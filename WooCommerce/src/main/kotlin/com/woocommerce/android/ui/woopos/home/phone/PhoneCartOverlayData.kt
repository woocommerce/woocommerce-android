package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.runtime.compositionLocalOf

data class PhoneCartOverlayData(
    val quantities: Map<Long, Int> = emptyMap(),
    val couponIdsInCart: Set<Long> = emptySet(),
    val onRemoveCoupon: (Long) -> Unit = {},
)

val LocalPhoneCartOverlay = compositionLocalOf { PhoneCartOverlayData() }
