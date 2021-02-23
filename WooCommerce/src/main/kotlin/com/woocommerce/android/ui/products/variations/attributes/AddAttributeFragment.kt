package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAddAttributeBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductGlobalAttribute
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAddAttribute
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.SkeletonView

class AddAttributeFragment : BaseProductFragment(R.layout.fragment_add_attribute) {
    companion object {
        const val TAG: String = "AddAttributeFragment"
        private const val LIST_STATE_KEY = "list_state"
    }

    private var layoutManager: LayoutManager? = null
    private val skeletonView = SkeletonView()

    private var _binding: FragmentAddAttributeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddAttributeBinding.bind(view)

        setHasOptionsMenu(true)
        initializeViews(savedInstanceState)
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductAddAttribute())
        return false
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

        binding.attributeEditText.setOnEditorActionListener { _, actionId, event ->
            val attributeName = binding.attributeEditText.text?.toString() ?: ""
            if (attributeName.isNotBlank()) {
                viewModel.addLocalAttribute(attributeName)
            }
            true
        }

        viewModel.fetchGlobalAttributes()
    }

    private fun setupObservers() {
        viewModel.globalAttributeList.observe(viewLifecycleOwner, Observer {
            showAttributes(it)
        })

        viewModel.globalAttributeViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitProductAddAttribute -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
    }

    override fun getFragmentTitle() = getString(R.string.product_add_attribute)

    private fun showAttributes(globalAttributes: List<ProductGlobalAttribute>) {
        val adapter: CombinedAttributeListAdapter
        if (binding.attributeList.adapter == null) {
            adapter = CombinedAttributeListAdapter(viewModel::onAttributeListItemClick)
            binding.attributeList.adapter = adapter
        } else {
            adapter = binding.attributeList.adapter as CombinedAttributeListAdapter
        }

        adapter.setAttributeList(
            localAttributes = viewModel.getProductDraftAttributes().filter { it.isLocalAttribute },
            globalAttributes = globalAttributes
        )
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.attributeList, R.layout.skeleton_simple_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }
}
