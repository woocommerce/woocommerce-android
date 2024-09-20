package com.woocommerce.android.ui.blaze.campaigs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewFragment
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlazeCampaignListFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            hasShadow = false
        )

    @Inject
    lateinit var blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher

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
        blazeCampaignCreationDispatcher.attachFragment(this, BlazeFlowSource.CAMPAIGN_LIST)

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().popBackStack()
                is BlazeCampaignListViewModel.LaunchBlazeCampaignCreation -> openBlazeCreationFlow()
                is BlazeCampaignListViewModel.ShowCampaignDetails -> openCampaignDetails(event.campaignId)
            }
        }
        handleResults()
    }

    private fun openBlazeCreationFlow() {
        lifecycleScope.launch {
            blazeCampaignCreationDispatcher.startCampaignCreation(source = BlazeFlowSource.CAMPAIGN_LIST)
        }
    }

    private fun openCampaignDetails(url: String) {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalBlazeCampaignDetailWebViewFragment(campaignId = url)
        )
    }

    private fun handleResults() {
        handleResult<BlazeAction>(BlazeCampaignDetailWebViewFragment.BLAZE_WEBVIEW_RESULT) {
            viewModel.onBlazeCampaignWebViewAction(it)
        }
    }
}
