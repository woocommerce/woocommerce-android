package com.woocommerce.android.apifaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.findNavController
import com.woocommerce.android.apifaker.ui.ApiFakerNavHost
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.LegacyWooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus

class ApiFakerHostFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                LegacyWooThemeWithBackground {
                    ApiFakerNavHost(
                        onExit = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}
