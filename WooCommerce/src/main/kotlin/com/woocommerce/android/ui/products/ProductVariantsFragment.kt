package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_product_variants.*
import javax.inject.Inject

class ProductVariantsFragment : BaseFragment() {
    companion object {
        const val TAG: String = "ProductVariantsFragment"
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var viewModel: ProductVariantsViewModel
    private lateinit var productVariantsAdapter: ProductVariantsAdapter

    private val skeletonView = SkeletonView()

    private val navArgs: ProductVariantsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_variants, container, false)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onDestroyView() {
        // hide the skeleton view if fragment is destroyed
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
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ProductVariantsViewModel::class.java).also {
            setupObservers(it)
        }

        viewModel.start(navArgs.remoteProductId)
    }

    private fun setupObservers(viewModel: ProductVariantsViewModel) {
        viewModel.isSkeletonShown.observe(this, Observer {
            showSkeleton(it)
        })

        viewModel.productVariantList.observe(this, Observer {
            showProductVariants(it)
        })

        viewModel.showSnackbarMessage.observe(this, Observer {
            uiMessageResolver.showSnack(it)
        })

        viewModel.isRefreshing.observe(this, Observer {
            productVariantsRefreshLayout.isRefreshing = it
        })

        viewModel.exit.observe(this, Observer {
            activity?.onBackPressed()
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        productVariantsAdapter = ProductVariantsAdapter(activity)
        with(productVariantsList) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productVariantsAdapter
            addItemDecoration(AlignedDividerDecoration(
                    activity, DividerItemDecoration.VERTICAL, R.id.variantOptionName, clipToMargin = false
            ))
        }

        productVariantsRefreshLayout?.apply {
            setColorSchemeColors(
                    ContextCompat.getColor(activity, R.color.colorPrimary),
                    ContextCompat.getColor(activity, R.color.colorAccent),
                    ContextCompat.getColor(activity, R.color.colorPrimaryDark)
            )
            scrollUpChild = productVariantsList
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.PRODUCT_VARIANTS_PULLED_TO_REFRESH)
                viewModel.refreshProductVariants(navArgs.remoteProductId)
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.product_variations)

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(productVariantsList, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showProductVariants(productVariants: List<ProductVariant>) {
        productVariantsAdapter.setProductVariantList(productVariants)
    }
}
