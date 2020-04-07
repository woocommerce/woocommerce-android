package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.ProductShippingClassAdapter.ShippingClassAdapterListener
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_product_shipping_class_list.*
import javax.inject.Inject

/**
 * Dialog which displays a list of product shipping classes
 */
class ProductShippingClassFragment : BaseFragment(), ShippingClassAdapterListener {
    companion object {
        const val TAG = "ProductShippingClassFragment"
        const val ARG_SELECTED_SHIPPING_CLASS_SLUG = "selected-shipping-class-slug"
        const val ARG_SELECTED_SHIPPING_CLASS_ID = "selected-shipping-class-id"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ProductShippingClassViewModel by viewModels { viewModelFactory }

    private val navArgs: ProductShippingClassFragmentArgs by navArgs()

    private var shippingClassAdapter: ProductShippingClassAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_shipping_class_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        shippingClassAdapter = ProductShippingClassAdapter(
                requireActivity(),
                this,
                navArgs.productShippingClassSlug
        )

        with(recycler) {
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
                shippingClassAdapter?.shippingClassList = it!!
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.product_shipping_class)

    override fun onShippingClassClicked(shippingClass: ShippingClass?) {
        val bundle = Bundle()
        bundle.putString(ARG_SELECTED_SHIPPING_CLASS_SLUG, shippingClass?.slug ?: "")
        bundle.putLong(ARG_SELECTED_SHIPPING_CLASS_ID, shippingClass?.remoteShippingClassId ?: 0L)
        requireActivity().navigateBackWithResult(
                RequestCodes.PRODUCT_SHIPPING_CLASS,
                bundle,
                R.id.nav_host_fragment_main,
                R.id.productShippingFragment
        )
    }

    override fun onRequestLoadMore() {
        viewModel.loadShippingClasses(loadMore = true)
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
