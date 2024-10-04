package com.woocommerce.android.ui.blaze.creation.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.selector.ProductSelectorFragment
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BlazeCampaignCreationIntroFragment : BaseFragment() {
    private val viewModel: BlazeCampaignCreationIntroViewModel by viewModels()

    @Inject
    lateinit var blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BlazeCampaignCreationIntroScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        blazeCampaignCreationDispatcher.attachFragment(this, BlazeFlowSource.INTRO_VIEW)
        handleEvents()
        handleResults()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is BlazeCampaignCreationIntroViewModel.ShowCampaignCreationForm ->
                    startBlazeCampaignCreationFlow(event.source, event.productId)

                is BlazeCampaignCreationIntroViewModel.ShowProductSelector ->
                    startBlazeCampaignCreationFlow(BlazeFlowSource.INTRO_VIEW)

                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun startBlazeCampaignCreationFlow(source: BlazeFlowSource, productId: Long? = null) {
        lifecycleScope.launch {
            blazeCampaignCreationDispatcher.startCampaignCreation(
                productId = productId,
                source = source
            )
        }
    }

    private fun handleResults() {
        handleResult<Collection<SelectedItem>>(ProductSelectorFragment.PRODUCT_SELECTOR_RESULT) {
            viewModel.onProductSelected(it.first().id)
        }
    }
}
