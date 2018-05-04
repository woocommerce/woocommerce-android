package com.woocommerce.android.ui.base

import org.wordpress.android.fluxc.model.SiteModel

interface BaseView<T> {
    fun getSelectedSite(): SiteModel?
}
