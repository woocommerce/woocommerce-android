package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.ui.products.ProductShippingClassAdapter.ShippingClassAdapterListener
import kotlinx.android.synthetic.main.fragment_product_shipping_class_list.*

/**
 * Dialog which displays a list of product shipping classes
 */
class ProductShippingClassFragment : BaseProductFragment(), ShippingClassAdapterListener {
    companion object {
        const val TAG = "ProductShippingClassFragment"
    }

    private var adapter: ProductShippingClassAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_shipping_class_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recycler?.addItemDecoration(
                DividerItemDecoration(
                        requireActivity(),
                        DividerItemDecoration.VERTICAL
                )
        )
        recycler.layoutManager = LinearLayoutManager(requireActivity())
        adapter = ProductShippingClassAdapter(
                requireActivity(),
                this,
                viewModel.getProduct().shippingClassSlug
        )
        recycler.adapter = adapter

        viewModel.loadShippingClasses()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun setupObservers() {
        viewModel.productShippingClassViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isLoadingProgressShown.takeIfNotEqualTo(old?.isLoadingProgressShown) {
                showLoadingProgress(new.isLoadingProgressShown)
            }
            new.isLoadingMoreProgressShown.takeIfNotEqualTo(old?.isLoadingMoreProgressShown) {
                showLoadingMoreProgress(new.isLoadingMoreProgressShown)
            }
            new.shippingClassList.takeIfNotEqualTo(old?.shippingClassList) {
                adapter?.shippingClassList = it!!
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.product_shipping_class)

    override fun onShippingClassClicked(shippingClass: ShippingClass?) {
        viewModel.updateProductDraft(shippingClass = shippingClass?.slug)
        findNavController().navigateUp()
    }

    override fun onRequestLoadMore() {
        viewModel.loadShippingClasses(loadMore = true)
    }

    override fun onRequestAllowBackPress(): Boolean {
        // we always return true here because the fragment is left as soon as the user chooses a shipping class
        return true
    }

    private fun showLoadingProgress(show: Boolean) {
        if (show) {
            loadingProgress.show()
        } else {
            loadingProgress.hide()
        }
    }

    private fun showLoadingMoreProgress(show: Boolean) {
        if (show) {
            loadingMoreProgress.show()
        } else {
            loadingMoreProgress.hide()
        }
    }
}
