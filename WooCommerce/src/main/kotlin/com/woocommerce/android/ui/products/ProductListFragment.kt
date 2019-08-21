package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.ProductListAdapter.OnProductClickListener
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_product_list.*
import kotlinx.android.synthetic.main.wc_empty_view.*
import javax.inject.Inject

class ProductListFragment : TopLevelFragment(), OnProductClickListener {
    companion object {
        val TAG: String = ProductListFragment::class.java.simpleName
        fun newInstance() = ProductListFragment()
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var viewModel: ProductListViewModel
    private lateinit var productAdapter: ProductListAdapter

    private val skeletonView = SkeletonView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productAdapter = ProductListAdapter(activity!!, this)
        productsRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
        productsRecycler.adapter = productAdapter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_product_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // TODO this is temporary until we have a real product list
        empty_view.visibility = View.VISIBLE
        empty_view_text.text = "Some day this will be a beautiful new products list..."
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ProductListViewModel::class.java).also {
            setupObservers(it)
        }
        viewModel.start()
    }

    private fun setupObservers(viewModel: ProductListViewModel) {
        viewModel.isSkeletonShown.observe(this, Observer {
            showSkeleton(it)
        })

        viewModel.productList.observe(this, Observer {
            showProductList(it)
        })

        viewModel.showSnackbarMessage.observe(this, Observer {
            uiMessageResolver.showSnack(it)
        })

        /*viewModel.exit.observe(this, Observer {
            activity?.onBackPressed()
        })*/
    }

    override fun getFragmentTitle() = getString(R.string.products)

    override fun refreshFragmentState() {
        // TODO
    }

    override fun onReturnedFromChildFragment() {
        // TODO
    }

    override fun scrollToTop() {
        productsRecycler.smoothScrollToPosition(0)
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(productsContainer, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showProductList(products: List<Product>) {
        // TODO
    }

    override fun onProductClick(remoteProductId: Long) {
        // TODO
    }
}
