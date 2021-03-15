package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAddAttributeTermsBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAddAttributeTerms
import com.woocommerce.android.ui.products.variations.attributes.AttributeTermsListAdapter.OnTermListener
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.DraggableItemTouchHelper
import com.woocommerce.android.widgets.SkeletonView

/**
 * This fragment contains two lists of product attribute terms. Thee\ first is a list of terms from
 * local (product-based) attributes, the second is a list of terms from global (store-wide) attributes
 */
class AddAttributeTermsFragment : BaseProductFragment(R.layout.fragment_add_attribute_terms) {
    companion object {
        const val TAG: String = "AddAttributeTermsFragment"
        private const val LIST_STATE_KEY_ASSIGNED = "list_state_assigned"
        private const val LIST_STATE_KEY_GLOBAL = "list_state_global"
    }

    private var layoutManagerAssigned: LinearLayoutManager? = null
    private var layoutManagerGlobal: LinearLayoutManager? = null

    private var _binding: FragmentAddAttributeTermsBinding? = null
    private val binding get() = _binding!!

    private val navArgs: AddAttributeTermsFragmentArgs by navArgs()
    private val skeletonView = SkeletonView()

    private val itemTouchHelper by lazy {
        DraggableItemTouchHelper(
            dragDirs = ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            onMove = { from, to ->
                assignedTermsAdapter.swapItems(from, to)
            }
        )
    }

    private lateinit var assignedTermsAdapter: AttributeTermsListAdapter
    private lateinit var globalTermsAdapter: AttributeTermsListAdapter

    /**
     * This is the listener attached to the list of assigned terms
     */
    private val assignedTermListener by lazy {
        object : OnTermListener {
            override fun onTermClick(termName: String) {}

            /**
             * If the user removed a global term from the assigned term list, we need to return it to the
             * global term list
             */
            override fun onTermDelete(termName: String) {
                viewModel.getProductDraftAttributes().find {
                    it.isGlobalAttribute && it.id == navArgs.attributeId
                }?.let { attribute ->
                    attribute.terms.find {
                        it == termName
                    }
                }?.let { term ->
                    globalTermsAdapter.addTerm(termName)
                }

                viewModel.removeAttributeTermFromDraft(navArgs.attributeId, navArgs.attributeName, termName)
                checkViews()
            }
        }
    }

    /**
     * This is the listener attached to the list of global terms
     */
    private val globalTermListener by lazy {
        object : OnTermListener {
            override fun onTermClick(termName: String) {
                addTerm(termName)
            }

            override fun onTermDelete(termName: String) {}
        }
    }

    private val isGlobalAttribute
        get() = navArgs.attributeId != 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddAttributeTermsBinding.bind(view)

        initializeViews(savedInstanceState)
        setupObservers()
        getAttributeTerms()
    }

    private fun getAttributeTerms() {
        // if this is a global attribute, fetch the attribute's terms
        if (isGlobalAttribute) {
            viewModel.fetchGlobalAttributeTerms(navArgs.attributeId)
        }

        // get the attribute terms for attributes already assigned to this product
        showAssignedTerms(viewModel.getProductDraftAttributeTerms(navArgs.attributeId, navArgs.attributeName))
    }

    override fun onDestroyView() {
        viewModel.resetGlobalAttributeTerms()
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        /**
         * TODO: we save attribute changes to the backend here only for testing purposes. Down the road we'll do this
         * before variations are generated (because they can't be generated until the attributes are saved)
         */
        viewModel.saveAttributeChanges()
        viewModel.onBackButtonClicked(ExitProductAddAttributeTerms())
        return false
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        layoutManagerAssigned?.let {
            outState.putParcelable(LIST_STATE_KEY_ASSIGNED, it.onSaveInstanceState())
        }
        layoutManagerGlobal?.let {
            outState.putParcelable(LIST_STATE_KEY_GLOBAL, it.onSaveInstanceState())
        }
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        layoutManagerAssigned = initializeRecycler(binding.assignedTermList, showIcons = true)
        assignedTermsAdapter = binding.assignedTermList.adapter as AttributeTermsListAdapter
        assignedTermsAdapter.setOnTermListener(assignedTermListener)
        savedInstanceState?.getParcelable<Parcelable>(LIST_STATE_KEY_ASSIGNED)?.let {
            layoutManagerAssigned!!.onRestoreInstanceState(it)
        }

        layoutManagerGlobal = initializeRecycler(binding.globalTermList, showIcons = false)
        globalTermsAdapter = binding.globalTermList.adapter as AttributeTermsListAdapter
        globalTermsAdapter.setOnTermListener(globalTermListener)
        savedInstanceState?.getParcelable<Parcelable>(LIST_STATE_KEY_GLOBAL)?.let {
            layoutManagerGlobal!!.onRestoreInstanceState(it)
        }

        binding.termEditText.setOnEditorActionListener { _, actionId, event ->
            val termName = binding.termEditText.text?.toString() ?: ""
            if (termName.isNotBlank() && !assignedTermsAdapter.containsTerm(termName)) {
                addTerm(termName)
                binding.termEditText.text?.clear()
            }
            true
        }
    }

    private fun initializeRecycler(recycler: RecyclerView, showIcons: Boolean): LinearLayoutManager {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        recycler.layoutManager = layoutManager

        if (showIcons) {
            recycler.adapter = AttributeTermsListAdapter(showIcons)
            itemTouchHelper.attachToRecyclerView(recycler)
        } else {
            recycler.adapter = AttributeTermsListAdapter(showIcons)
        }

        recycler.addItemDecoration(
            AlignedDividerDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL,
                R.id.variationOptionName,
                clipToMargin = false
            )
        )

        return layoutManager
    }

    private fun setupObservers() {
        viewModel.attributeTermsList.observe(viewLifecycleOwner, Observer {
            showGlobalAttributeTerms(it)
        })

        viewModel.globalAttributeTermsViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) {
                showSkeleton(it)
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitProductAddAttributeTerms -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
    }

    override fun getFragmentTitle() = navArgs.attributeName

    /**
     * Show the list of terms already assigned to the product attribute
     */
    private fun showAssignedTerms(termNames: List<String>) {
        if (termNames.isEmpty()) {
            binding.assignedTermList.isVisible = false
        } else {
            binding.assignedTermList.isVisible = true
            assignedTermsAdapter.termNames = ArrayList<String>().also { it.addAll(termNames) }
        }
    }

    /**
     * Triggered by fetching the list of terms for global attributes
     */
    private fun showGlobalAttributeTerms(terms: List<ProductAttributeTerm>) {
        if (terms.isEmpty()) {
            globalTermsAdapter.clear()
        } else {
            // build a list of term names, excluding ones that are already assigned
            val assignedTermNames = assignedTermsAdapter.termNames
            val termNames = ArrayList<String>()
            terms.forEach { term ->
                if (!assignedTermNames.contains(term.name)) {
                    termNames.add(term.name)
                }
            }

            globalTermsAdapter.termNames = termNames
        }

        checkViews()
    }

    private fun checkViews() {
        binding.assignedTermList.isVisible = !assignedTermsAdapter.isEmpty()
        binding.textExistingOption.isVisible = !globalTermsAdapter.isEmpty()
    }

    /**
     * User entered a new term or tapped a global term
     */
    private fun addTerm(termName: String) {
        // add the term to the list of assigned terms
        assignedTermsAdapter.addTerm(termName)

        // remove it from the list of global terms
        if (isGlobalAttribute) {
            globalTermsAdapter.removeTerm(termName)
        }

        viewModel.addAttributeTermToDraft(navArgs.attributeId, navArgs.attributeName, termName)
        checkViews()
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.globalTermContainer, R.layout.skeleton_simple_list, true)
        } else {
            skeletonView.hide()
        }
        checkViews()
    }
}
