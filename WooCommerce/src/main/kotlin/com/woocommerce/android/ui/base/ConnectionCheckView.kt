package com.woocommerce.android.ui.base

interface ConnectionCheckView {
    fun isNetworkConnected(): Boolean
    fun showNetworkConnectivityError()
}
