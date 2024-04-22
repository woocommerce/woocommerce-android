package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAddAttributeBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.parcelable
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.ProductGlobalAttribute
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitProductAddAttribute
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class AddAttributeFragment : BaseProductFragment(R.layout.fragment_add_attribute) {
    companion object {
        const val TAG: String = "AddAttributeFragment"
        private const val LIST_STATE_KEY = "list_state"
        private const val ID_ADD_ATTRIBUTES = 1
    }

    private var layoutManager: LayoutManager? = null
    private val skeletonView = SkeletonView()
    private var moveNextMenuItem: MenuItem? = null

    private val navArgs: AddAttributeFragmentArgs by navArgs()

    private var _binding: FragmentAddAttributeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddAttributeBinding.bind(view)

        initializeViews(savedInstanceState)
        setupObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked(ExitProductAddAttribute)
        return false
    }

    override fun onResume() {
        super.onResume()
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
            moveNextMenuItem = toolbar.menu.add(Menu.FIRST, ID_ADD_ATTRIBUTES, Menu.FIRST, R.string.next).apply {
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                isVisible = false
            }
        }
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            ID_ADD_ATTRIBUTES -> {
                viewModel.saveAttributeChanges()
                AddAttributeFragmentDirections
                    .actionAddAttributeFragmentToAttributeListFragment(isVariationCreation = true)
                    .run { findNavController().navigateSafely(this) }
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

        binding.attributeEditText.setOnEditorActionListener { _, _, _ ->
            val attributeName = binding.attributeEditText.text?.toString() ?: ""
            if (attributeName.isNotBlank()) {
                binding.attributeEditText.text?.clear()
                viewModel.addLocalAttribute(attributeName, navArgs.isVariationCreation)
            }
            true
        }

        viewModel.fetchGlobalAttributes()

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_add_attribute),
            onMenuItemSelected = ::onMenuItemSelected,
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onBackButtonClicked(ExitProductAddAttribute)
                }
                onCreateMenu(toolbar)
            }
        )
    }

    private fun setupObservers() {
        viewModel.globalAttributeList.observe(
            viewLifecycleOwner
        ) {
            showAttributes(it)
        }

        viewModel.globalAttributeViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
        }

        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is ExitProductAddAttribute -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
    }

    /**
     * Called after fetching global attributes, sets the adapter to show a combined list of the
     * passed global attributes and the existing draft local attributes
     */
    private fun showAttributes(globalAttributes: List<ProductGlobalAttribute>) {
        moveNextMenuItem?.isVisible = navArgs.isVariationCreation and viewModel.productDraftAttributes.isNotEmpty()

        val adapter: AddAttributeAdapter
        if (binding.attributeList.adapter == null) {
            adapter = AddAttributeAdapter { attributeId, attributeName ->
                viewModel.onAttributeListItemClick(attributeId, attributeName, navArgs.isVariationCreation)
            }
            binding.attributeList.adapter = adapter
        } else {
            adapter = binding.attributeList.adapter as AddAttributeAdapter
        }

        val allDraftAttributes = viewModel.productDraftAttributes
        val localDraftAttributes = allDraftAttributes.filter { it.isLocalAttribute }
        val globalDraftAttributes = allDraftAttributes.filter { it.isGlobalAttribute }

        // returns the list of draft terms for the passed global attribute
        fun getGlobalDraftTerms(attributeId: Long): List<String> {
            return globalDraftAttributes.firstOrNull {
                it.id == attributeId
            }?.terms ?: emptyList()
        }

        adapter.refreshAttributeList(
            ArrayList<ProductAttribute>().also { allAttributes ->
                // add the list of global attributes along with any terms each global attribute has in the product draft
                allAttributes.addAll(
                    ArrayList<ProductAttribute>().also {
                        it.addAll(
                            globalAttributes.map { attribute ->
                                attribute.toProductAttributeForDisplay(getGlobalDraftTerms(attribute.remoteId))
                            }
                        )
                    }
                )

                // add local draft attributes then sort the combined list by name
                allAttributes.addAll(localDraftAttributes)
                allAttributes.sortBy { it.name.lowercase(Locale.getDefault()) }
            }
        )

        (globalAttributes.isNotEmpty() or localDraftAttributes.isNotEmpty()).let { shouldBeVisible ->
            binding.attributeSelectionHint.isVisible = shouldBeVisible
            binding.divider.isVisible = shouldBeVisible
        }
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.attributeList, R.layout.skeleton_simple_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }
}
