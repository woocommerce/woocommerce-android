package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAttributeListBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.parcelable
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitProductAttributeList
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttributeListFragment : BaseProductFragment(R.layout.fragment_attribute_list) {
    companion object {
        const val TAG: String = "AttributeListFragment"
        private const val LIST_STATE_KEY = "list_state"
        private const val ID_ATTRIBUTE_LIST = 1
    }

    private var layoutManager: LayoutManager? = null
    private var nextMenuItem: MenuItem? = null

    private val navArgs: AttributeListFragmentArgs by navArgs()

    private var _binding: FragmentAttributeListBinding? = null
    private val binding get() = _binding!!

    private val isGeneratingVariation
        get() = navArgs.isVariationCreation and viewModel.productDraftAttributes.isNotEmpty()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAttributeListBinding.bind(view)

        initializeViews(savedInstanceState)
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        nextMenuItem?.isVisible = isGeneratingVariation
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        layoutManager?.let {
            outState.putParcelable(LIST_STATE_KEY, it.onSaveInstanceState())
        }
    }

    private fun onCreateMenu(toolbar: Toolbar) {
        if (navArgs.isVariationCreation) {
            nextMenuItem = toolbar.menu.add(Menu.FIRST, ID_ATTRIBUTE_LIST, Menu.FIRST, R.string.next).apply {
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                isVisible = isGeneratingVariation
            }
        }
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            ID_ATTRIBUTE_LIST -> {
                AttributeListFragmentDirections.actionAttributeListFragmentToAttributesAddedFragment()
                    .apply { findNavController().navigateSafely(this) }
                true
            }
            else -> false
        }
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        this.layoutManager = layoutManager

        savedInstanceState?.parcelable<Parcelable>(LIST_STATE_KEY)?.let {
            layoutManager.onRestoreInstanceState(it)
        }

        binding.attributeList.layoutManager = layoutManager
        binding.attributeList.itemAnimator = null

        binding.addAttributeButton.setOnClickListener {
            if (navArgs.isVariationCreation) {
                findNavController().navigateUp()
            } else {
                viewModel.onAddAttributeButtonClick()
            }
        }

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_variation_attributes),
            onMenuItemSelected = ::onMenuItemSelected,
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onBackButtonClicked(ExitProductAttributeList)
                }
                onCreateMenu(toolbar)
            }
        )
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitProductAttributeList -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }

        viewModel.attributeList.observe(viewLifecycleOwner) {
            showAttributes(it)
        }

        viewModel.loadProductDraftAttributes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductAttributeList)
        return false
    }

    private fun showAttributes(attributes: List<ProductAttribute>) {
        val adapter: AttributeListAdapter
        if (binding.attributeList.adapter == null) {
            adapter = AttributeListAdapter { attributeId, attributeName ->
                viewModel.onAttributeListItemClick(attributeId, attributeName, navArgs.isVariationCreation)
            }
            binding.attributeList.adapter = adapter
        } else {
            adapter = binding.attributeList.adapter as AttributeListAdapter
        }

        adapter.refreshAttributeList(attributes)
    }
}
