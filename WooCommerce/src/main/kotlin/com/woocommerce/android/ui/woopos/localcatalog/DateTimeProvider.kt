package com.woocommerce.android.ui.woopos.localcatalog

import javax.inject.Inject

class DateTimeProvider @Inject constructor() : DateTimeProviderInterface {
    override fun now(): Long = System.currentTimeMillis()
}

interface DateTimeProviderInterface {
    fun now(): Long
}
