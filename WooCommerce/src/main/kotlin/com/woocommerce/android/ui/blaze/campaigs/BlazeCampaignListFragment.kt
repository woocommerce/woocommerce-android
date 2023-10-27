package com.woocommerce.android.ui.blaze.campaigs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignListFragment : BaseFragment() {

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            hasShadow = false
        )

    private val viewModel: BlazeCampaignListViewModel by viewModels()

    override fun getFragmentTitle() = getString(R.string.blaze_campaign_list_title)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    BlazeCampaignListScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().popBackStack()
                is BlazeCampaignListViewModel.LaunchBlazeCampaignCreation -> openBlazeWebView(event.url, event.source)
                is BlazeCampaignListViewModel.ShowCampaignDetails -> openCampaignDetails(
                    event.url,
                    event.urlToTriggerExit
                )
            }
        }
    }

    private fun openBlazeWebView(url: String, source: BlazeFlowSource) {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalBlazeCampaignCreationFragment(
                urlToLoad = url,
                source = source
            )
        )
    }

    private fun openCampaignDetails(url: String, urlToTriggerExit: String) {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                urlToLoad = url,
                urlsToTriggerExit = arrayOf(urlToTriggerExit),
                title = getString(R.string.blaze_campaign_details_title)
            )
        )
    }
}
