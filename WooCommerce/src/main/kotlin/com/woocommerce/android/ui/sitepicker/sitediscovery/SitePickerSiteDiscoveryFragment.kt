package com.woocommerce.android.ui.sitepicker.sitediscovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.sitepicker.sitediscovery.SitePickerSiteDiscoveryViewModel.CreateZendeskTicket
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SitePickerSiteDiscoveryFragment : BaseFragment() {
    private val viewModel: SitePickerSiteDiscoveryViewModel by viewModels()

    @Inject
    lateinit var zendeskHelper: ZendeskHelper

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireActivity()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    SitePickerSiteDiscoveryScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                CreateZendeskTicket -> {
                    zendeskHelper.createNewTicket(requireActivity(), Origin.LOGIN_SITE_ADDRESS, null)
                }
            }
        }
    }
}
