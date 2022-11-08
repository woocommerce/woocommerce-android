package com.woocommerce.android.ui.login.jetpackinstallation

import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus

class JetpackConfigurationContainerFragment: BaseFragment(R.layout.fragment_jetpack_configuration_container) {
    companion object {
        const val TAG = "JetpackConfigurationContainerFragment"
    }

    // Hide the Toolbar, this fragment is to be reused between MainActivity and LoginActivity,
    // so we'll leave it to each child fragment to handle its toolbar
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden
}
