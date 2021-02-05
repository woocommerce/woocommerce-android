package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAttributeListBinding
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAttributeList
import com.woocommerce.android.widgets.AlignedDividerDecoration

class AttributeListFragment : BaseProductFragment(R.layout.fragment_attribute_list) {
    companion object {
        const val TAG: String = "AttributeListFragment"
        private const val LIST_STATE_KEY = "list_state"
    }

    private var layoutManager: LayoutManager? = null
    private val navArgs: AttributeListFragmentArgs by navArgs()

    private var _binding: FragmentAttributeListBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAttributeListBinding.bind(view)

        setHasOptionsMenu(true)
        initializeViews(savedInstanceState)
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress() = viewModel.onBackButtonClicked(ExitProductAttributeList())

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
    }

    private fun setupObservers() {
        viewModel.attributeList.observe(viewLifecycleOwner, Observer {
            showAttributes(it)
        })
        viewModel.loadProductDraftAttributes()
    }

    override fun getFragmentTitle() = getString(R.string.product_variation_attributes)

    private fun showAttributes(attributes: List<Product.Attribute>) {
        val adapter: AttributeListAdapter
        if (binding.attributeList.adapter == null) {
            adapter = AttributeListAdapter(viewModel::onAttributeListItemClick)
            binding.attributeList.adapter = adapter
        } else {
            adapter = binding.attributeList.adapter as AttributeListAdapter
        }

        adapter.setAttributeList(attributes)
    }
}
