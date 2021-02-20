package com.woocommerce.android.ui.base

interface FabManager {
    fun showFabAnimated(contentDescription: Int, onClick: () -> Unit)
    fun hideFabAnimated()
    fun hideFabImmediately()
}
