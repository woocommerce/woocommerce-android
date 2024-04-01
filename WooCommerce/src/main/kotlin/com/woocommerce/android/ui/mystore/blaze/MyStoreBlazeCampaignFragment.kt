package com.woocommerce.android.ui.mystore.blaze

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.mystore.MyStoreFragment
import com.woocommerce.android.ui.mystore.MyStoreFragmentDirections
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyStoreBlazeCampaignFragment : Fragment() {
    companion object {
        const val TAG = "MyStoreBlazeCampaignFragment"
    }

    private val viewModel: MyStoreBlazeViewModel by viewModels()

    @Inject
    lateinit var blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return composeView {
            WooThemeWithBackground {
                viewModel.blazeViewState.observeAsState().value?.let { state ->
                    val isHidden = state is MyStoreBlazeViewModel.MyStoreBlazeCampaignState.Hidden

                    LaunchedEffect(key1 = isHidden) {
                        (requireParentFragment() as MyStoreFragment).setBlazeCampaignVisibility(!isHidden)
                    }

                    if (!isHidden) {
                        MyStoreBlazeView(
                            state = state,
                            onDismissBlazeView = viewModel::onBlazeViewDismissed
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blazeCampaignCreationDispatcher.attachFragment(this, BlazeFlowSource.MY_STORE_SECTION)
        handleEvents()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MyStoreBlazeViewModel.LaunchBlazeCampaignCreationUsingWebView -> openBlazeWebView(
                    url = event.url,
                    source = event.source
                )

                is MyStoreBlazeViewModel.LaunchBlazeCampaignCreation -> openBlazeCreationFlow(event.productId)

                is MyStoreBlazeViewModel.ShowAllCampaigns -> {
                    findNavController().navigateSafely(
                        MyStoreFragmentDirections.actionMyStoreToBlazeCampaignListFragment()
                    )
                }

                is MyStoreBlazeViewModel.ShowCampaignDetails -> {
                    findNavController().navigateSafely(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                            urlToLoad = event.url,
                            urlsToTriggerExit = arrayOf(event.urlToTriggerExit),
                            title = getString(R.string.blaze_campaign_details_title)
                        )
                    )
                }
            }
        }
    }

    private fun openBlazeCreationFlow(productId: Long?) {
        lifecycleScope.launch {
            blazeCampaignCreationDispatcher.startCampaignCreation(
                source = BlazeFlowSource.MY_STORE_SECTION,
                productId = productId
            )
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
}
