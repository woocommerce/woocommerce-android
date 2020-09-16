package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

@OpenClassOnDebug
class OrderDetailRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
)
