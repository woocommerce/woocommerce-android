package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class GroupedProductListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore
)
