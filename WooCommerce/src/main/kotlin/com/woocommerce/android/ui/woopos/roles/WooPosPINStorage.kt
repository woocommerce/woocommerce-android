package com.woocommerce.android.ui.woopos.roles

interface WooPosPINStorage {
    fun read(localSiteId: Int): ByteArray?
    fun write(localSiteId: Int, bytes: ByteArray?)
    fun clearAll()
}
