package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
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
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAttributeList
import com.woocommerce.android.widgets.AlignedDividerDecoration

class AttributeListFragment : BaseProductFragment(R.layout.fragment_attribute_list) {
    companion object {
        const val TAG: String = "AttributeListFragment"
        private const val LIST_STATE_KEY = "list_state"
        private const val ID_ATTRIBUTE_LIST = 1
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

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductAttributeList())
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        if (navArgs.isVariationCreation) {
            menu.add(Menu.FIRST, ID_ATTRIBUTE_LIST, Menu.FIRST, R.string.done).apply {
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                isVisible = true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            ID_ATTRIBUTE_LIST -> {
                AttributeListFragmentDirections
                    .actionAttributeListFragmentToVariationListFragment(
                        remoteProductId = viewModel.getRemoteProductId(),
                        isVariationCreation = true
                    ).run { findNavController().navigateSafely(this) }
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

        binding.attributeList.layoutManager = layoutManager
        binding.attributeList.itemAnimator = null
        binding.attributeList.addItemDecoration(AlignedDividerDecoration(
            requireContext(), DividerItemDecoration.VERTICAL, R.id.variationOptionName, clipToMargin = false
        ))

        binding.addAttributeButton.setOnClickListener {
            viewModel.onAddAttributeButtonClick()
        }
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitProductAttributeList -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })

        viewModel.attributeList.observe(viewLifecycleOwner, Observer {
            showAttributes(it)
        })

        viewModel.loadProductDraftAttributes()
    }

    override fun getFragmentTitle() = getString(R.string.product_variation_attributes)

    private fun showAttributes(attributes: List<ProductAttribute>) {
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
