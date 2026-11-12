package com.woocommerce.android.ui.woopos.roles

/**
 * Production implementations must encrypt records with a device-bound key and exclude them from backup and transfer.
 */
interface WooPosPINStorage {
    /** Returns null only for an absent record; unreadable or corrupt existing records must throw. */
    fun read(localSiteId: Int): ByteArray?
    fun write(localSiteId: Int, bytes: ByteArray?)

    /** A successful return makes all prior records unusable; otherwise this must throw. */
    fun clearAll()

    companion object {
        const val MAX_RECORD_BYTES = 1024
    }
}
