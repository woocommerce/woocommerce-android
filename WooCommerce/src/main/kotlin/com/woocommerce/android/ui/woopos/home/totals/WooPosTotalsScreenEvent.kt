package com.woocommerce.android.ui.woopos.home.totals

sealed interface WooPosTotalsScreenEvent {
    data object RequestFineLocationPermission : WooPosTotalsScreenEvent
}
