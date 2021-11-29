package com.woocommerce.android.ui.moremenu

import com.woocommerce.android.R
import com.woocommerce.android.ui.base.TopLevelFragment

class MoreMenuFragment : TopLevelFragment(R.layout.fragment_analytics) {
    override fun getFragmentTitle() = getString(R.string.more_menu)

    override fun shouldExpandToolbar(): Boolean = false

    override fun scrollToTop() {
        // Nothing until https://github.com/woocommerce/woocommerce-android/issues/5237 is done
        return
    }
}
    
