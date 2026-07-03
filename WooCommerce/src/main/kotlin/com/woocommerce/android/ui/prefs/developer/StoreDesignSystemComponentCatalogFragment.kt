package com.woocommerce.android.ui.prefs.developer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.designSystemComposeView
import com.woocommerce.android.ui.compose.designsystem.preview.WooDesignSystemComponentCatalogScreen
import com.woocommerce.android.ui.main.AppBarStatus

class StoreDesignSystemComponentCatalogFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return designSystemComposeView {
            WooDesignSystemComponentCatalogScreen(
                initialPath = "",
                onBackClick = { findNavController().navigateUp() },
            )
        }
    }
}
