package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.ProductShippingClassAdapter.ShippingClassAdapterListener
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_product_shipping_class_list.*
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import javax.inject.Inject

/**
 * Dialog which displays a list of product shipping classes
 */
class ProductShippingClassFragment : BaseFragment(), ShippingClassAdapterListener {
    companion object {
        const val TAG = "ProductShippingClassDialog"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: ProductShippingClassViewModel by viewModels { viewModelFactory }

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
        adapter = ProductShippingClassAdapter(requireActivity(), this)
        recycler.adapter = adapter
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
        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.showLoadingProgress.takeIfNotEqualTo(old?.showLoadingProgress) {
                showLoadingProgress(new.showLoadingProgress)
            }
            new.showLoadingMoreProgress.takeIfNotEqualTo(old?.showLoadingMoreProgress) {
                showLoadingMoreProgress(new.showLoadingMoreProgress)
            }
        }

        viewModel.productShippingClasses.observe(viewLifecycleOwner, Observer { shippingClasses ->
            adapter?.shippingClassList = shippingClasses
        })
    }

    override fun getFragmentTitle() = getString(R.string.product_shipping_class)

    override fun onShippingClassClicked(shippingClass: WCProductShippingClassModel?) {
        // TODO: a future PR should return the selected shipping class to the shipping fragment
        findNavController().navigateUp()
    }

    override fun onRequestLoadMore() {
        viewModel.loadProductShippingClasses(loadMore = true)
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
