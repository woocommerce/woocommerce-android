package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentBundleProductListBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.adapters.BundleProductListAdapter
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductBundleFragment : BaseFragment(R.layout.fragment_bundle_product_list) {
    private val viewModel: ProductBundleViewModel by viewModels()
    private val skeletonView = SkeletonView()

    private var _binding: FragmentBundleProductListBinding? = null
    private val binding get() = _binding!!

    private val productListAdapter: BundleProductListAdapter by lazy { BundleProductListAdapter() }

    override fun getFragmentTitle() = resources.getString(R.string.product_bundle)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBundleProductListBinding.bind(view)

        binding.productsRecycler.run {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = productListAdapter
            isMotionEventSplittingEnabled = false
            if (itemDecorationCount == 0) {
                addItemDecoration(
                    AlignedDividerDecoration(
                        context,
                        DividerItemDecoration.VERTICAL,
                        R.id.productInfoContainer
                    )
                )
            }
        }

        viewModel.productList.observe(viewLifecycleOwner) {
            productListAdapter.submitList(it)
        }
        viewModel.productListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
        }
    }

    private fun showSkeleton(show: Boolean) {
        binding.notice.isVisible = show.not()
        when (show) {
            true -> {
                skeletonView.show(binding.productsRecycler, R.layout.skeleton_product_list, delayed = true)
            }
            false -> skeletonView.hide()
        }
    }
}
