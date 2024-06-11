package com.woocommerce.android.ui.products.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.creation.configuration.EditProductConfigurationResult
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfigurationFragment.Companion.PRODUCT_CONFIGURATION_EDITED_RESULT
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfigurationFragment.Companion.PRODUCT_CONFIGURATION_RESULT
import com.woocommerce.android.ui.orders.creation.configuration.SelectProductConfigurationResult
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.ui.products.ProductNavigator
import com.woocommerce.android.ui.products.filter.ProductFilterResult
import com.woocommerce.android.ui.products.list.ProductListFragment.Companion.PRODUCT_FILTER_RESULT_KEY
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.ui.products.variations.picker.VariationPickerFragment
import com.woocommerce.android.ui.products.variations.picker.VariationPickerViewModel.VariationPickerResult
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorFragment
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.VariationSelectionResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProductSelectorFragment : BaseFragment() {
    companion object {
        const val PRODUCT_SELECTOR_RESULT = "product-selector-result"
    }

    @Inject lateinit var navigator: ProductNavigator

    private val viewModel: ProductSelectorViewModel by viewModels()
    private val sharedViewModel: ProductSelectorSharedViewModel by activityViewModels()

    override val activityAppBarStatus: AppBarStatus = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            id = R.id.product_selector_compose_view
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    ProductSelectorScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
        handleResults()
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitWithResult<*> -> {
                    navigateBackWithResult(
                        PRODUCT_SELECTOR_RESULT,
                        event.data as Collection<SelectedItem>
                    )
                }
                is ProductNavigationTarget -> navigator.navigate(this, event)
                is Exit -> findNavController().navigateUp()
            }
        }
        if (viewModel.selectionMode == ProductSelectorViewModel.SelectionMode.LIVE) {
            lifecycleScope.launch {
                sharedViewModel.selectedItems.collect {
                    viewModel.updateSelectedItems(it)
                }
            }
            lifecycleScope.launch {
                viewModel.selectedItems.collect {
                    sharedViewModel.updateSelectedItems(it)
                }
            }
            lifecycleScope.launch {
                sharedViewModel.isProductSelectionActive.collect {
                    viewModel.onProductSelectionStateChanged(it)
                }
            }
        }
    }

    private fun handleResults() {
        handleResult<VariationSelectionResult>(VariationSelectorFragment.VARIATION_SELECTOR_RESULT) {
            viewModel.onSelectedVariationsUpdated(it)
        }

        handleResult<VariationPickerResult>(VariationPickerFragment.VARIATION_PICKER_RESULT) {
            // This means we are in the single-selection mode, return result immediately
            navigateBackWithResult(
                PRODUCT_SELECTOR_RESULT,
                listOf(SelectedItem.ProductVariation(it.productId, it.variationId))
            )
        }

        handleResult<ProductFilterResult>(PRODUCT_FILTER_RESULT_KEY) { result ->
            viewModel.onFiltersChanged(
                stockStatus = result.stockStatus,
                productStatus = result.productStatus,
                productType = result.productType,
                productCategory = result.productCategory,
                productCategoryName = result.productCategoryName
            )
        }

        handleResult<SelectProductConfigurationResult>(PRODUCT_CONFIGURATION_RESULT) { result ->
            viewModel.onConfigurationSaved(result.productId, result.productConfiguration)
        }

        handleResult<EditProductConfigurationResult>(PRODUCT_CONFIGURATION_EDITED_RESULT) { result ->
            viewModel.onConfigurationEdited(result.productId, result.productConfiguration)
        }
    }
}
