package com.woocommerce.android.ui.login.jetpack

import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus

class JetpackActivationContainerFragment : BaseFragment(R.layout.fragment_login_jetpack_activation_container) {
    companion object {
        const val TAG = "JetpackConfigurationContainerFragment"
    }

    // Hide the Toolbar, this fragment is to be reused between MainActivity and LoginActivity,
    // so we'll leave it to each child fragment to handle its toolbar
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden
}
