package com.woocommerce.android.ui.blaze.creation.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.selector.ProductSelectorFragment
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignCreationStartFragment : BaseFragment() {
    private val viewModel: BlazeCampaignCreationStartViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleEvents()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            // TODO Implement loading view for making the AI prompt before showing the AD preview screen"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleResults()
    }

    private fun handleEvents() {
        // Use the fragment as the lifecycle owner since navigation might happen before the view is created
        viewModel.event.observe(this) { event ->
            when (event) {
                is BlazeCampaignCreationStartViewModel.ShowBlazeCampaignCreationIntro ->
                    navigateToBlazeCampaignCreationIntro(event.productId)

                is BlazeCampaignCreationStartViewModel.ShowProductSelectorScreen ->
                    navigateToProductSelectorScreen()

                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun handleResults() {
        handleResult<Collection<SelectedItem>>(ProductSelectorFragment.PRODUCT_SELECTOR_RESULT) {
            viewModel.onProductSelected(it.first().id)
        }
    }

    private fun navigateToBlazeCampaignCreationIntro(productId: Long) {
        findNavController().navigateSafely(
            directions = BlazeCampaignCreationStartFragmentDirections
                .actionBlazeCampaignCreationStartFragmentToBlazeCampaignCreationIntroFragment(productId),
            skipThrottling = true, // perform the navigation immediately
            navOptions = navOptions {
                popUpTo(R.id.blazeCampaignCreationStartFragment) { inclusive = true }
            }
        )
    }

    private fun navigateToProductSelectorScreen() {
        findNavController().navigateSafely(
            directions = BlazeCampaignCreationStartFragmentDirections
                .actionBlazeCampaignCreationStartFragmentToProductSelector(
                    selectionMode = ProductSelectorViewModel.SelectionMode.SINGLE,
                    selectionHandling = ProductSelectorViewModel.SelectionHandling.SIMPLE,
                    screenTitleOverride = getString(R.string.blaze_campaign_creation_product_selector_title),
                    ctaButtonTextOverride = getString(R.string.blaze_campaign_creation_product_selector_cta_button),
                    productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined
                ),
            skipThrottling = true // perform the navigation immediately
        )
    }
}
