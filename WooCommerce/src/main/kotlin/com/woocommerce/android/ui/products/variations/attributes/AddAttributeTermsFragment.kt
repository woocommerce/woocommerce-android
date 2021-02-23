package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAddAttributeTermsBinding
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAddAttributeTerms
import com.woocommerce.android.widgets.AlignedDividerDecoration

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddAttributeTermsBinding.bind(view)

        setHasOptionsMenu(true)
        initializeViews(savedInstanceState)
        setupObservers()

        getAttributeTerms()
    }

    private fun getAttributeTerms() {
        // if this is a global attribute, fetch the attribute's terms and exclude ones that are already assigned
        if (navArgs.attributeId != 0L) {
            viewModel.fetchGlobalAttributeTerms(navArgs.attributeId, excludeAssignedTerms = true)
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
    }

    private fun initializeRecycler(recycler: RecyclerView, showIcons: Boolean): LinearLayoutManager {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)

        recycler.layoutManager = layoutManager
        recycler.itemAnimator = null
        recycler.adapter = AttributeTermsListAdapter(showIcons)
        recycler.addItemDecoration(AlignedDividerDecoration(
            requireContext(), DividerItemDecoration.VERTICAL, R.id.variationOptionName, clipToMargin = false
        ))

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

    /**
     * Show the list of terms already assigned to the product attribute
     */
    fun showAssignedTerms(termNames: List<String>) {
        val adapter = binding.assignedTermList.adapter as AttributeTermsListAdapter
        adapter.setTerms(termNames)
    }

    /**
     * Triggered by fetching the list of terms for global attributes
     */
    private fun showGlobalAttributeTerms(terms: List<ProductAttributeTerm>) {
        // build a list of term names
        val termNames = ArrayList<String>()
        terms.forEach { term ->
            termNames.add(term.name)
        }

        val adapter = binding.globalTermList.adapter as AttributeTermsListAdapter
        adapter.setTerms(termNames)
    }
}
