package com.woocommerce.android.ui.google

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import javax.inject.Inject

// The Google Ads feature (available for Jetpack and Jetpack Connection Package sites) needs to open non-WordPress.com
// wp-admin URLs. To do so, it has to pick whether to use `WPComWebView` (which includes automatic login) or
// `ExitAwareWebView` (which doesn't).
// While technically Jetpack Connection Package sites can automatically login with `WPComWebView` too, there's a
// behavior where the login redirect does not work properly (it redirects to WordPress.com instead), so it has to use
// `ExitAwareWebView` instead.
class CanUseAutoLoginWebview @Inject constructor(
    private val selectedSite: SelectedSite
) {
    operator fun invoke(): Boolean = selectedSite.get().connectionType == SiteConnectionType.Jetpack
}
