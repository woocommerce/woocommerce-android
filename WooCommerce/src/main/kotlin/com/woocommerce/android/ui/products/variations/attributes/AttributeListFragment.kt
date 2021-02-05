package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.os.Parcelable
import android.view.View
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
import com.woocommerce.android.databinding.FragmentAttributeListBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.variations.attributes.AttributeListViewModel.ShowAddAttributeScreen
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import javax.inject.Inject

class AttributeListFragment : BaseFragment(R.layout.fragment_attribute_list),
    OnLoadMoreListener {
    companion object {
        const val TAG: String = "AttributeListFragment"
        private const val LIST_STATE_KEY = "list_state"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: AttributeListViewModel by viewModels { viewModelFactory }
    private var layoutManager: LayoutManager? = null
    private val navArgs: AttributeListFragmentArgs by navArgs()

    private var _binding: FragmentAttributeListBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAttributeListBinding.bind(view)

        setHasOptionsMenu(true)
        initializeViews(savedInstanceState)
        initializeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        layoutManager?.let {
            outState.putParcelable(LIST_STATE_KEY, it.onSaveInstanceState())
        }
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        this.layoutManager = layoutManager

        savedInstanceState?.getParcelable<Parcelable>(LIST_STATE_KEY)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        binding.attributeList.layoutManager = layoutManager
        binding.attributeList.itemAnimator = null
        binding.attributeList.addItemDecoration(AlignedDividerDecoration(
            requireContext(), DividerItemDecoration.VERTICAL, R.id.variationOptionName, clipToMargin = false
        ))

        binding.attributeListRefreshLayout.apply {
            scrollUpChild = binding.attributeList
            setOnRefreshListener {
                // TODO: AnalyticsTracker.track
                viewModel.refreshAttributes(navArgs.remoteProductId)
            }
        }

        binding.addAttributeButton.setOnClickListener {
            viewModel.onAddButtonClick(navArgs.remoteProductId)
        }
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
        viewModel.start(navArgs.remoteProductId)
    }

    private fun setupObservers(viewModel: AttributeListViewModel) {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                binding.attributeListRefreshLayout.isRefreshing = it
            }
        }

        viewModel.attributeList.observe(viewLifecycleOwner, Observer {
            showAttributes(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowAddAttributeScreen -> openAddAttributeScreen(event.remoteProductId)
                is Exit -> activity?.onBackPressed()
            }
        })
    }

    private fun openAddAttributeScreen(remoteProductId: Long) {
        val action = AttributeListFragmentDirections.actionAttributeListFragmentToAddAttributeFragment(
            remoteProductId
        )
        findNavController().navigateSafely(action)
    }

    override fun getFragmentTitle() = getString(R.string.product_variation_attributes)

    private fun showAttributes(attributes: List<Product.Attribute>) {
        val adapter: AttributeListAdapter
        if (binding.attributeList.adapter == null) {
            adapter = AttributeListAdapter(viewModel::onItemClick)
            binding.attributeList.adapter = adapter
        } else {
            adapter = binding.attributeList.adapter as AttributeListAdapter
        }

        adapter.setAttributeList(attributes)
    }

    override fun onRequestLoadMore() {
        // TODO currently not supported by FluxC
    }
}
