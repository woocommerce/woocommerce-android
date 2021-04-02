package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentVariationListBinding
import com.woocommerce.android.databinding.ProductPropertyWarningLayoutBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowAttributeList
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ShowVariationDetail
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.SkeletonView
import javax.inject.Inject

class VariationListFragment : BaseFragment(R.layout.fragment_variation_list),
    OnLoadMoreListener {
    companion object {
        const val TAG: String = "VariationListFragment"
        private const val LIST_STATE_KEY = "list_state"
        private const val ID_EDIT_ATTRIBUTES = 1
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: VariationListViewModel by viewModels { viewModelFactory }

    private val skeletonView = SkeletonView()
    private var layoutManager: LayoutManager? = null

    private val navArgs: VariationListFragmentArgs by navArgs()

    private var _binding: FragmentVariationListBinding? = null
    private val binding get() = _binding!!

    private var isVariationCreation = false

    // this is an included layout
    private var _warningBinding: ProductPropertyWarningLayoutBinding? = null
    private val warningBinding get() = _warningBinding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentVariationListBinding.bind(view)
        _warningBinding = binding.variationVisibilityWarning

        setHasOptionsMenu(true)
        initializeViews(savedInstanceState)
        initializeViewModel()
        isVariationCreation = navArgs.isVariationCreation
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
        _binding = null
        _warningBinding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        if(isVariationCreation) {
            viewModel.onAddEditAttributesClick(isVariationCreation = true)
            isVariationCreation = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        layoutManager?.let {
            outState.putParcelable(LIST_STATE_KEY, it.onSaveInstanceState())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if (FeatureFlag.ADD_EDIT_VARIATIONS.isEnabled()) {
            val mnuEditAttr = menu.add(Menu.NONE, ID_EDIT_ATTRIBUTES, Menu.NONE, R.string.product_variations_edit_attr)
            mnuEditAttr.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            mnuEditAttr.isVisible = false
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        if (FeatureFlag.ADD_EDIT_VARIATIONS.isEnabled()) {
            menu.findItem(ID_EDIT_ATTRIBUTES)?.isVisible = !viewModel.isEmpty()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            ID_EDIT_ATTRIBUTES -> {
                viewModel.onAddEditAttributesClick()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        this.layoutManager = layoutManager

        savedInstanceState?.getParcelable<Parcelable>(LIST_STATE_KEY)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        binding.variationList.layoutManager = layoutManager
        binding.variationList.itemAnimator = null
        binding.variationList.addItemDecoration(AlignedDividerDecoration(
            requireContext(), DividerItemDecoration.VERTICAL, R.id.variationOptionName, clipToMargin = false
        ))

        binding.variationListRefreshLayout.apply {
            scrollUpChild = binding.variationList
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.PRODUCT_VARIANTS_PULLED_TO_REFRESH)
                viewModel.refreshVariations(navArgs.remoteProductId)
            }
        }
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
        viewModel.start(navArgs.remoteProductId)
        binding.firstVariationView.setOnClickListener {
            viewModel.onAddEditAttributesClick(isVariationCreation = true)
        }
    }

    private fun setupObservers(viewModel: VariationListViewModel) {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                binding.variationListRefreshLayout.isRefreshing = it
            }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) {
                binding.loadMoreProgress.isVisible = it
            }
            new.isWarningVisible?.takeIfNotEqualTo(old?.isWarningVisible) { showWarning(it) }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { isEmptyViewVisible ->
                if (FeatureFlag.ADD_EDIT_VARIATIONS.isEnabled()) {
                    binding.firstVariationView.updateVisibility(
                        shouldBeVisible = isEmptyViewVisible,
                        showButton = true
                    )
                } else {
                    binding.emptyView.updateVisibility(
                        shouldBeVisible = isEmptyViewVisible,
                        showButton = false
                    )
                }
                requireActivity().invalidateOptionsMenu()
            }
        }

        viewModel.variationList.observe(viewLifecycleOwner, Observer {
            showVariations(it, viewModel.viewStateLiveData.liveData.value?.parentProduct)
            requireActivity().invalidateOptionsMenu()
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowVariationDetail -> openVariationDetail(event.variation)
                is ShowAttributeList -> openAttributeList(event.isVariationCreation)
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> activity?.onBackPressed()
            }
        })
    }

    private fun showWarning(isVisible: Boolean) {
        warningBinding.root.isVisible = isVisible
    }

    private fun openVariationDetail(variation: ProductVariation) {
        val action = VariationListFragmentDirections.actionVariationListFragmentToVariationDetailFragment(
            variation
        )
        findNavController().navigateSafely(action)
    }

    private fun openAttributeList(isVariationCreation: Boolean) {
        val action = VariationListFragmentDirections
            .actionVariationListFragmentToAttributeListFragment(isVariationCreation)
        findNavController().navigateSafely(action)
        Log.d("Testing", "navigation to attribute list called")
    }

    override fun getFragmentTitle() = getString(R.string.product_variations)

    override fun onRequestLoadMore() {
        viewModel.onLoadMoreRequested(navArgs.remoteProductId)
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.variationList, R.layout.skeleton_product_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showVariations(variations: List<ProductVariation>, parentProduct: Product?) {
        val adapter: VariationListAdapter
        if (binding.variationList.adapter == null) {
            adapter = VariationListAdapter(
                requireContext(),
                GlideApp.with(this),
                this,
                parentProduct,
                viewModel::onItemClick
            )
            binding.variationList.adapter = adapter
        } else {
            adapter = binding.variationList.adapter as VariationListAdapter
        }

        adapter.setVariationList(variations)
    }
}
