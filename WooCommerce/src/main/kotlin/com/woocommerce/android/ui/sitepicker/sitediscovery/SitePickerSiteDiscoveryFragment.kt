package com.woocommerce.android.ui.sitepicker.sitediscovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.sitepicker.sitediscovery.SitePickerSiteDiscoveryViewModel.CreateZendeskTicket
import com.woocommerce.android.ui.sitepicker.sitediscovery.SitePickerSiteDiscoveryViewModel.NavigateToHelpScreen
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SitePickerSiteDiscoveryFragment : BaseFragment() {
    companion object {
        const val SITE_PICKER_SITE_ADDRESS_RESULT = "site-url"
    }

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
                is NavigateToHelpScreen -> {
                    startActivity(HelpActivity.createIntent(requireContext(), Origin.LOGIN_SITE_ADDRESS, null))
                }
                is ExitWithResult<*> -> navigateBackWithResult(SITE_PICKER_SITE_ADDRESS_RESULT, event.data)
                is Exit -> findNavController().navigateUp()
            }
        }
    }
}
