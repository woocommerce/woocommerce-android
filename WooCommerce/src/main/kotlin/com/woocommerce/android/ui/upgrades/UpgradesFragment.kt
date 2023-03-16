package com.woocommerce.android.ui.upgrades

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment.Companion.WEBVIEW_DISMISSED
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.plans.di.StartUpgradeFlowFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@ExperimentalFoundationApi
class UpgradesFragment : BaseFragment() {

    override fun getFragmentTitle() = getString(R.string.upgrades_title)

    private val viewModel: UpgradesViewModel by viewModels()

    @Inject
    lateinit var startUpgradeFlowFactory: StartUpgradeFlowFactory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WooThemeWithBackground {
                    UpgradesScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                UpgradesViewModel.UpgradesEvent.OpenSubscribeNow -> {
                    startUpgradeFlowFactory.create(navController = findNavController()).invoke()
                }
            }
        }
        handleNotice(WPComWebViewFragment.WEBVIEW_RESULT) {
            viewModel.onPlanUpgraded()
        }
        handleNotice(WEBVIEW_DISMISSED) {
            // tracks event here
        }
    }
}
