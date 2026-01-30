package com.woocommerce.android.ui.woopos.localcatalog

import javax.inject.Inject

class WooPosDateTimeProvider @Inject constructor() {
    fun now(): Long = System.currentTimeMillis()
}
