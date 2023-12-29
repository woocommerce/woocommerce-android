package com.woocommerce.android.ui.blaze.creation.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.selector.ProductSelectorFragment
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignCreationDispatcherFragment : BaseFragment() {
    private val viewModel: BlazeCampaignCreationDispatcherViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleEvents()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Return a non-null View to be able to handle result from the ProductSelectorFragment
        return View(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleResults()
    }

    private fun handleEvents() {
        // Use the fragment as the lifecycle owner since navigation might happen before the view is created
        viewModel.event.observe(this) { event ->
            when (event) {
                is BlazeCampaignCreationDispatcherViewModel.ShowBlazeCampaignCreationIntro ->
                    navigateToBlazeCampaignCreationIntro(event.productId)

                is BlazeCampaignCreationDispatcherViewModel.ShowBlazeCampaignCreationAdForm ->
                    // TODO update when the AD form is implemented
                    Toast.makeText(
                        requireContext(),
                        "This will show the AD form for product ${event.productId}",
                        Toast.LENGTH_SHORT
                    ).show()

                is BlazeCampaignCreationDispatcherViewModel.ShowProductSelectorScreen ->
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
            directions = BlazeCampaignCreationDispatcherFragmentDirections
                .actionBlazeCampaignCreationStartFragmentToBlazeCampaignCreationIntroFragment(productId),
            skipThrottling = true, // perform the navigation immediately
            navOptions = navOptions {
                popUpTo(R.id.blazeCampaignCreationDispatcherFragment) { inclusive = true }
            }
        )
    }

    private fun navigateToProductSelectorScreen() {
        findNavController().navigateSafely(
            directions = BlazeCampaignCreationDispatcherFragmentDirections
                .actionBlazeCampaignCreationStartFragmentToProductSelector(
                    selectionMode = ProductSelectorViewModel.SelectionMode.SINGLE,
                    selectionHandling = ProductSelectorViewModel.SelectionHandling.SIMPLE,
                    screenTitleOverride = getString(R.string.blaze_campaign_creation_product_selector_title),
                    ctaButtonTextOverride = getString(R.string.blaze_campaign_creation_product_selector_cta_button),
                    productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined
                ),
            skipThrottling = true, // perform the navigation immediately,
        )
    }
}
