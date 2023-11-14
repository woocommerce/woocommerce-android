package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductShippingClassListBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import dagger.hilt.android.AndroidEntryPoint

/**
 * Dialog which displays a list of product shipping classes
 */
@AndroidEntryPoint
class ProductShippingClassFragment : BaseFragment(R.layout.fragment_product_shipping_class_list) {
    companion object {
        const val TAG = "ProductShippingClassFragment"
        const val SELECTED_SHIPPING_CLASS_RESULT = "selected-shipping-class"
    }

    private val viewModel: ProductShippingClassViewModel by viewModels()

    private val navArgs: ProductShippingClassFragmentArgs by navArgs()

    private var shippingClassAdapter: ProductShippingClassAdapter? = null

    private var _binding: FragmentProductShippingClassListBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductShippingClassListBinding.bind(view)
        setupObservers()

        shippingClassAdapter = ProductShippingClassAdapter(
            this::onShippingClassClicked,
            this::onLoadMoreRequested
        )

        with(binding.recycler) {
            addItemDecoration(
                DividerItemDecoration(
                    requireActivity(),
                    DividerItemDecoration.VERTICAL
                )
            )
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = shippingClassAdapter
        }

        viewModel.loadShippingClasses()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isLoadingProgressShown.takeIfNotEqualTo(old?.isLoadingProgressShown) {
                showLoadingProgress(new.isLoadingProgressShown)
            }
            new.isLoadingMoreProgressShown.takeIfNotEqualTo(old?.isLoadingMoreProgressShown) {
                showLoadingMoreProgress(new.isLoadingMoreProgressShown)
            }
            new.shippingClassList?.takeIfNotEqualTo(old?.shippingClassList) {
                shippingClassAdapter?.update(it, navArgs.productShippingClassId)
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitWithResult<*> -> navigateBackWithResult(SELECTED_SHIPPING_CLASS_RESULT, event.data)
                else -> event.isHandled = false
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.product_shipping_class)

    private fun onShippingClassClicked(shippingClass: ShippingClass) {
        viewModel.onShippingClassClicked(shippingClass)
    }

    private fun onLoadMoreRequested() {
        viewModel.loadShippingClasses(loadMore = true)
    }

    private fun showLoadingProgress(show: Boolean) {
        if (show) {
            binding.loadingProgress.show()
        } else {
            binding.loadingProgress.hide()
        }
    }

    private fun showLoadingMoreProgress(show: Boolean) {
        if (show) {
            binding.loadingMoreProgress.show()
        } else {
            binding.loadingMoreProgress.hide()
        }
    }
}
