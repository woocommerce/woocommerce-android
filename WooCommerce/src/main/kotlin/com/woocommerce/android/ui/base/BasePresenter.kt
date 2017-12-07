package com.woocommerce.android.ui.base

interface BasePresenter<in T> {
    fun takeView(view: T)
    fun dropView()
}
