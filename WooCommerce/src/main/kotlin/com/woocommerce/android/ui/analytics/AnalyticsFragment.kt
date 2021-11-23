package com.woocommerce.android.ui.analytics

import com.woocommerce.android.R
import com.woocommerce.android.ui.base.TopLevelFragment

/**
 * This would be an empty fragment until issue
 * https://github.com/woocommerce/woocommerce-android/issues/5237  gets finished
 */
class AnalyticsFragment : TopLevelFragment(R.layout.fragment_analytics) {
    override fun getFragmentTitle() = getString(R.string.analytics)

    override fun shouldExpandToolbar(): Boolean = false

    override fun scrollToTop() {
        // Nothing until https://github.com/woocommerce/woocommerce-android/issues/5237 is done
        return
    }
}
