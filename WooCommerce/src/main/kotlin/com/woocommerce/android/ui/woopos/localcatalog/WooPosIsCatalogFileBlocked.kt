package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import javax.inject.Inject

/**
 * Reads the persisted "catalog file blocked by the host" flag. The flag is set when a full sync fails
 * because the host blocks the generated catalog file (HTTP 403 / HTML body) and cleared on the first
 * successful full sync. POS entry uses it to skip the blocking download wait that would otherwise
 * repeat every time, while the background worker keeps retrying.
 */
class WooPosIsCatalogFileBlocked @Inject constructor(
    private val syncTimestampManager: WooPosSyncTimestampManager,
) {
    suspend operator fun invoke(): Boolean = syncTimestampManager.isCatalogFileBlocked()
}
