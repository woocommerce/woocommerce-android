package com.woocommerce.android.ui.google

import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

// The Google for WooCommerce feature uses a webview to display campaign creation or dashboard. When first opened, the
// webview can automatically logs user in (thus saving them some steps) if the site is a WordPress.com Atomic site.
// For .org Jetpack or Jetpack Connection Package sites, the redirection does not work correctly
// (it logs in then redirects to WordPress.com instead), so we should not use automatic login for these sites.
class CanUseAutoLoginWebview @Inject constructor(private val selectedSite: SelectedSite) {
    operator fun invoke(): Boolean = selectedSite.get().isWPComAtomic
}
