package com.woocommerce.android.ui.orders.wooshippinglabels.hazmat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.hazmat.WooShippingLabelHazmatFormViewModel.Companion.HAZMAT_CATEGORY_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.hazmat.WooShippingLabelHazmatFormViewModel.Companion.KEY_HAZMAT_CATEGORY_SELECTOR_RESULT
import com.woocommerce.android.ui.orders.wooshippinglabels.hazmat.WooShippingLabelHazmatFormViewModel.OnHazmatCategorySelected
import com.woocommerce.android.ui.orders.wooshippinglabels.hazmat.WooShippingLabelHazmatFormViewModel.OnSelectCategoryClicked
import com.woocommerce.android.ui.orders.wooshippinglabels.hazmat.WooShippingLabelHazmatFormViewModel.OnUrlSelected
import com.woocommerce.android.ui.searchfilter.SearchFilterItem
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooShippingLabelHazmatFormFragment : BaseFragment(), BackPressListener {
    private val viewModel: WooShippingLabelHazmatFormViewModel by viewModels()

    override fun getFragmentTitle() = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    Surface {
                        WooShippingLabelHazmatFormScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindResultHandlers()
        bindEventListener()
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackPressed()
        return false
    }

    private fun bindResultHandlers() {
        handleDialogResult<String>(
            key = KEY_HAZMAT_CATEGORY_SELECTOR_RESULT,
            entryId = R.id.wooShippingLabelHazmatFormFragment
        ) { hazmatSelection ->
            val selectedCategory = ShippingLabelHazmatCategory.valueOf(hazmatSelection)
            viewModel.onHazmatCategorySelected(selectedCategory)
        }
    }

    private fun bindEventListener() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OnUrlSelected -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                is OnSelectCategoryClicked -> showHazmatCategoryPicker()
                is OnHazmatCategorySelected -> navigateBackWithResult(
                    key = HAZMAT_CATEGORY_RESULT,
                    result = event.selectedCategory?.name.orEmpty()
                )
            }
        }
    }

    private fun showHazmatCategoryPicker() {
        WooShippingLabelHazmatFormFragmentDirections
            .actionWooShippingLabelHazmatFormFragmentToHazmatCategorySelector(
                title = getString(R.string.woo_shipping_labels_hazmat_info_select_category),
                hint = getString(R.string.woo_shipping_labels_hazmat_search_hint),
                requestKey = KEY_HAZMAT_CATEGORY_SELECTOR_RESULT,
                items = ShippingLabelHazmatCategory.entries
                    .map {
                        SearchFilterItem(
                            name = getString(it.stringResourceID),
                            value = it.toString()
                        )
                    }.toTypedArray(),
            ).let { findNavController().navigateSafely(it) }
    }
}
