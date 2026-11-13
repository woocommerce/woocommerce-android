package com.woocommerce.android.ui.woopos.roles.pin

interface WooPosPINCredentialSource {
    // Use the database's local site ID, including for stores without a WordPress.com site ID.
    // Empty credentials mean protection is off; storage failure must keep the caller locked.
    suspend fun credentials(localSiteId: Int): Result<List<WooPosPINCredential>>
}
