package com.woocommerce.android.ui.blaze.creation.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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
class BlazeCampaignCreationIntroFragment : BaseFragment() {
    private val viewModel: BlazeCampaignCreationIntroViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BlazeCampaignCreationIntroScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
        handleResults()
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is BlazeCampaignCreationIntroViewModel.ShowCampaignCreationForm -> {
                    // TODO update when the AD form is implemented
                    Toast.makeText(
                        requireContext(),
                        "This will show the campaign creation form for product ${event.productId}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is BlazeCampaignCreationIntroViewModel.ShowProductSelector -> {
                    navigateToProductSelectorScreen()
                }

                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
            }
        }
    }

    private fun handleResults() {
        handleResult<Collection<SelectedItem>>(ProductSelectorFragment.PRODUCT_SELECTOR_RESULT) {
            viewModel.onProductSelected(it.first().id)
        }
    }

    private fun navigateToProductSelectorScreen() {
        findNavController().navigateSafely(
            BlazeCampaignCreationIntroFragmentDirections
                .actionBlazeCampaignCreationIntroFragmentToNavGraphProductSelector(
                    selectionMode = ProductSelectorViewModel.SelectionMode.SINGLE,
                    selectionHandling = ProductSelectorViewModel.SelectionHandling.SIMPLE,
                    screenTitleOverride = getString(R.string.blaze_campaign_creation_product_selector_title),
                    ctaButtonTextOverride = getString(R.string.blaze_campaign_creation_product_selector_cta_button),
                    productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.Undefined
                )
        )
    }
}
