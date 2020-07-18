package com.woocommerce.android.ui.products.tags

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductTags
import com.woocommerce.android.ui.products.tags.ProductTagsAdapter.OnProductTagClickListener
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import kotlinx.android.synthetic.main.fragment_product_tags.*

class ProductTagsFragment : BaseProductFragment(), OnLoadMoreListener, OnProductTagClickListener {
    private lateinit var productTagsAdapter: ProductTagsAdapter

    private val skeletonView = SkeletonView()
    private var progressDialog: CustomProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_tags, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.product_tags)

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.onProductTagDoneMenuActionClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
        viewModel.fetchProductTags()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        productTagsAdapter = ProductTagsAdapter(activity.baseContext, this, this)
        with(productTagsRecycler) {
            layoutManager = LinearLayoutManager(activity)
            adapter = productTagsAdapter
            addItemDecoration(
                AlignedDividerDecoration(
                    activity, DividerItemDecoration.VERTICAL, R.id.tagName, clipToMargin = false
                ))
        }

        productTagsLayout?.apply {
            scrollUpChild = productTagsRecycler
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.PRODUCT_TAGS_PULLED_TO_REFRESH)
                viewModel.refreshProductTags()
            }
        }

        addProductTagView.setOnEditorActionListener {
            viewModel.onProductTagAdded(it)
            updateSelectedTags()
            true
        }
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.productTagsViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { productTagsLayout.isRefreshing = it }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) { showProgressDialog(it) }
            new.shouldDisplayDoneMenuButton?.takeIfNotEqualTo(old?.shouldDisplayDoneMenuButton) {
                showUpdateMenuItem(it)
            }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isEmptyViewVisible ->
                if (isEmptyViewVisible) {
                    WooAnimUtils.fadeIn(empty_view)
                    empty_view.show(EmptyViewType.PRODUCT_TAG_LIST)
                } else {
                    WooAnimUtils.fadeOut(empty_view)
                    empty_view.hide()
                }
            }
        }

        viewModel.productTags.observe(viewLifecycleOwner, Observer {
            showProductTags(it)
        })

        viewModel.addedProductTags.observe(viewLifecycleOwner, Observer {
            addTags(it, this)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitProductTags -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
    }

    private fun showProductTags(productTags: List<ProductTag>) {
        productTagsAdapter.setProductTags(productTags)
        updateSelectedTags()
    }

    private fun updateSelectedTags() {
        val product = requireNotNull(viewModel.getProduct().productDraft)
        addTags(product.tags, this)
    }

    private fun addTags(tags: List<ProductTag>, listener: OnProductTagClickListener) {
        addProductTagView.addSelectedTags(tags, listener)
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(productTagsRecycler, R.layout.skeleton_product_tags, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(R.string.product_add_tag_dialog_title),
                getString(R.string.product_update_dialog_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showLoadMoreProgress(show: Boolean) {
        loadMoreTagsProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreTagsRequested()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitProductTags())
    }

    override fun onProductTagAdded(productTag: ProductTag) {
        viewModel.onProductTagSelected(productTag)
        updateSelectedTags()
    }

    override fun onProductTagRemoved(productTag: ProductTag) {
        viewModel.onProductTagSelectionRemoved(productTag)
        addProductTagView.removeSelectedTag(productTag)
        changesMade()
    }
}
