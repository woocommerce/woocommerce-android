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
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAddAttributeTerms
import com.woocommerce.android.ui.products.variations.attributes.AttributeTermsListAdapter.OnTermClickListener
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.DraggableItemTouchHelper

/**
 * This fragment contains two lists of product attribute terms. Thee\ first is a list of terms from
 * local (product-based) attributes, the second is a list of terms from global (store-wide) attributes
 */
class AddAttributeTermsFragment : BaseProductFragment(R.layout.fragment_add_attribute_terms), OnTermClickListener {
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

    private val itemTouchHelper by lazy {
        DraggableItemTouchHelper(
            dragDirs = ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            onMove = { from, to ->
                getAssignedTermsAdapter().swapItems(from, to)
            }
        )
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
        val terms = getAssignedTermsAdapter().termNames
        viewModel.setProductDraftAttributeTerms(
            navArgs.attributeId,
            navArgs.attributeName,
            terms
        )
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
        savedInstanceState?.getParcelable<Parcelable>(LIST_STATE_KEY_ASSIGNED)?.let {
            layoutManagerAssigned!!.onRestoreInstanceState(it)
        }

        layoutManagerGlobal = initializeRecycler(binding.globalTermList, showIcons = false)
        savedInstanceState?.getParcelable<Parcelable>(LIST_STATE_KEY_GLOBAL)?.let {
            layoutManagerGlobal!!.onRestoreInstanceState(it)
        }

        binding.termEditText.setOnEditorActionListener { _, actionId, event ->
            val termName = binding.termEditText.text?.toString() ?: ""
            if (termName.isNotBlank() && !getAssignedTermsAdapter().containsTerm(termName)) {
                addTerm(termName, saveToBackend = isGlobalAttribute)
                binding.termEditText.text?.clear()
            }
            true
        }
    }

    private fun initializeRecycler(recycler: RecyclerView, showIcons: Boolean): LinearLayoutManager {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        recycler.layoutManager = layoutManager

        if (showIcons) {
            recycler.adapter = AttributeTermsListAdapter(showIcons, itemTouchHelper)
            itemTouchHelper.attachToRecyclerView(recycler)
        } else {
            recycler.adapter = AttributeTermsListAdapter(showIcons, onTermClick = this)
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

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitProductAddAttributeTerms -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
    }

    override fun getFragmentTitle() = navArgs.attributeName

    private fun getAssignedTermsAdapter() = binding.assignedTermList.adapter as AttributeTermsListAdapter

    private fun getGlobalTermsAdapter() = binding.globalTermList.adapter as AttributeTermsListAdapter

    /**
     * Show the list of terms already assigned to the product attribute
     */
    private fun showAssignedTerms(termNames: List<String>) {
        if (termNames.isEmpty()) {
            binding.assignedTermList.isVisible = false
        } else {
            binding.assignedTermList.isVisible = true
            getAssignedTermsAdapter().termNames = ArrayList<String>().also { it.addAll(termNames) }
        }
    }

    /**
     * Triggered by fetching the list of terms for global attributes
     */
    private fun showGlobalAttributeTerms(terms: List<ProductAttributeTerm>) {
        if (terms.isEmpty()) {
            getGlobalTermsAdapter().clear()
        } else {
            // build a list of term names, excluding ones that are already assigned
            val assignedTermNames = getAssignedTermsAdapter().termNames
            val termNames = ArrayList<String>()
            terms.forEach { term ->
                if (!assignedTermNames.contains(term.name)) {
                    termNames.add(term.name)
                }
            }

            getGlobalTermsAdapter().termNames = termNames
        }

        checkViews()
    }

    private fun checkViews() {
        binding.assignedTermList.isVisible = !getAssignedTermsAdapter().isEmpty()
        binding.globalTermContainer.isVisible = !getGlobalTermsAdapter().isEmpty()
    }

    /**
     * User entered a new term or tapped a global term, saveToBackend will only be true
     * if the user entered a new term and this is a global attribute
     */
    private fun addTerm(termName: String, saveToBackend: Boolean) {
        // add the term to the list of assigned terms
        getAssignedTermsAdapter().addTerm(termName)

        // remove it from the list of global terms
        if (isGlobalAttribute) {
            getGlobalTermsAdapter().removeTerm(termName)
        }

        if (saveToBackend) {
            // TODO progress spinner or progress dialog?
            viewModel.addGlobalAttributeTerm(navArgs.attributeId, termName)
        }

        checkViews()
    }

    /**
     * Called by the global adapter when a term is clicked
     */
    override fun onTermClick(termName: String) {
        addTerm(termName, saveToBackend = false)
    }
}
