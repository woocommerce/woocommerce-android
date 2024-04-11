package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAddAttributeTermsBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.parcelable
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.details.ProductDetailViewModel.ProductExitEvent.ExitProductAddAttributeTerms
import com.woocommerce.android.ui.products.variations.attributes.AddAttributeTermsViewModel.LoadingState.Appending
import com.woocommerce.android.ui.products.variations.attributes.AddAttributeTermsViewModel.LoadingState.Loading
import com.woocommerce.android.ui.products.variations.attributes.AttributeTermsListAdapter.OnTermListener
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.widgets.DraggableItemTouchHelper
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint

/**
 * This fragment contains two lists of product attribute terms. Thee\ first is a list of terms from
 * local (product-based) attributes, the second is a list of terms from global (store-wide) attributes
 */
@AndroidEntryPoint
class AddAttributeTermsFragment : BaseProductFragment(R.layout.fragment_add_attribute_terms) {
    companion object {
        const val TAG: String = "AddAttributeTermsFragment"
        private const val LIST_STATE_KEY_ASSIGNED = "list_state_assigned"
        private const val LIST_STATE_KEY_GLOBAL = "list_state_global"
        private const val KEY_RENAMED_ATTRIBUTE_NAME = "renamed_attribute_name"
        private const val KEY_IS_CONFIRM_REMOVE_DIALOG_SHOWING = "is_remove_dialog_showing"
    }

    private val termsViewModel: AddAttributeTermsViewModel by viewModels()

    private var layoutManagerAssigned: LinearLayoutManager? = null
    private var layoutManagerGlobal: LinearLayoutManager? = null

    private var _binding: FragmentAddAttributeTermsBinding? = null
    private val binding get() = _binding!!

    private val navArgs: AddAttributeTermsFragmentArgs by navArgs()
    private val skeletonView = SkeletonView()

    private var isConfirmRemoveDialogShowing = false
    private var renamedAttributeName: String? = null
    private var moveNextMenuItem: MenuItem? = null

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

            override fun onTermMoved(fromTermName: String, toTermName: String) {
                viewModel.swapProductDraftAttributeTerms(
                    navArgs.attributeId,
                    navArgs.attributeName,
                    fromTermName,
                    toTermName
                )
            }

            /**
             * If the user removed a global term from the assigned term list, we need to return it to the
             * global term list
             */
            override fun onTermDelete(termName: String) {
                viewModel.productDraftAttributes.find {
                    it.isGlobalAttribute && it.id == navArgs.attributeId
                }?.let { attribute ->
                    attribute.terms.find {
                        it == termName
                    }
                }?.let {
                    globalTermsAdapter.addTerm(termName)
                }

                viewModel.removeAttributeTermFromDraft(navArgs.attributeId, attributeName, termName)
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
            override fun onTermMoved(fromTermName: String, toTermName: String) {}
        }
    }

    private val isGlobalAttribute
        get() = navArgs.attributeId != 0L

    private val attributeName
        get() = renamedAttributeName ?: navArgs.attributeName

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddAttributeTermsBinding.bind(view)

        initializeViews(savedInstanceState)
        setupObservers()
        setupResultHandlers()
        getAttributeTerms()

        savedInstanceState?.let { bundle ->
            if (bundle.getBoolean(KEY_IS_CONFIRM_REMOVE_DIALOG_SHOWING)) {
                confirmRemoveAttribute()
            }
            if (bundle.containsKey(KEY_RENAMED_ATTRIBUTE_NAME)) {
                renamedAttributeName = bundle.getString(KEY_RENAMED_ATTRIBUTE_NAME)
            }
        }
    }

    private fun getAttributeTerms() {
        // if this is a global attribute, fetch the attribute's terms
        if (isGlobalAttribute) {
            termsViewModel.onFetchAttributeTerms(navArgs.attributeId)
        }

        // get the attribute terms for attributes already assigned to this product
        showAssignedTerms(viewModel.getProductDraftAttributeTerms(navArgs.attributeId, attributeName))
    }

    override fun onDestroyView() {
        termsViewModel.resetGlobalAttributeTerms()
        super.onDestroyView()
        _binding = null
    }

    private fun onCreateMenu(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.menu_attribute_terms)
        onPrepareMenu(toolbar.menu)
    }

    private fun onPrepareMenu(menu: Menu) {
        /**
         * we only want to show the Next menu item if we're under the creation of
         * the first variation for a Variable Product
         */
        moveNextMenuItem = menu.findItem(R.id.menu_next)

        /** we don't want to show the Remove menu item if this is new attribute
         * or if we're under the First variation creation flow
         */
        menu.findItem(R.id.menu_remove)?.isVisible = !navArgs.isNewAttribute && !navArgs.isVariationCreation

        /** we don't want to show the Rename menu item if this is new attribute or a global attribute,
         * or if we're under the First variation creation flow
         */
        menu.findItem(R.id.menu_rename)?.isVisible =
            !navArgs.isNewAttribute &&
            !isGlobalAttribute &&
            !navArgs.isVariationCreation
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_remove -> {
                confirmRemoveAttribute()
                true
            }
            R.id.menu_rename -> {
                viewModel.onRenameAttributeButtonClick(attributeName)
                true
            }
            R.id.menu_next -> {
                viewModel.saveAttributeChanges()
                AddAttributeTermsFragmentDirections.actionAddAttributeTermsFragmentToAttributeListFragment(
                    isVariationCreation = navArgs.isVariationCreation
                ).run { findNavController().navigateSafely(this) }
                true
            }
            else -> false
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        if (navArgs.isNewAttribute and assignedTermsAdapter.isEmpty()) {
            viewModel.removeAttributeFromDraft(navArgs.attributeId, attributeName)
        }
        saveChangesAndReturn()
        return false
    }

    private fun saveChangesAndReturn() {
        // TODO We probably want to push changes in the main attribute list screen rather than here
        viewModel.saveAttributeChanges()
        viewModel.onBackButtonClicked(ExitProductAddAttributeTerms)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        binding.termEditText.showKeyboard(selectAll = true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (isConfirmRemoveDialogShowing) {
            outState.putBoolean(KEY_IS_CONFIRM_REMOVE_DIALOG_SHOWING, true)
        }
        renamedAttributeName?.let {
            outState.putString(KEY_RENAMED_ATTRIBUTE_NAME, it)
        }
        layoutManagerAssigned?.let {
            outState.putParcelable(LIST_STATE_KEY_ASSIGNED, it.onSaveInstanceState())
        }
        layoutManagerGlobal?.let {
            outState.putParcelable(LIST_STATE_KEY_GLOBAL, it.onSaveInstanceState())
        }
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        layoutManagerAssigned = initializeRecycler(
            binding.assignedTermList,
            enableDragAndDrop = !isGlobalAttribute,
            enableDeleting = true,
            supportsLoadMore = false
        )
        assignedTermsAdapter = binding.assignedTermList.adapter as AttributeTermsListAdapter
        assignedTermsAdapter.setOnTermListener(assignedTermListener)
        savedInstanceState?.parcelable<Parcelable>(LIST_STATE_KEY_ASSIGNED)?.let {
            layoutManagerAssigned!!.onRestoreInstanceState(it)
        }

        layoutManagerGlobal = initializeRecycler(
            binding.globalTermList,
            enableDragAndDrop = false,
            enableDeleting = false,
            supportsLoadMore = true
        )
        globalTermsAdapter = binding.globalTermList.adapter as AttributeTermsListAdapter
        globalTermsAdapter.setOnTermListener(globalTermListener)
        savedInstanceState?.parcelable<Parcelable>(LIST_STATE_KEY_GLOBAL)?.let {
            layoutManagerGlobal!!.onRestoreInstanceState(it)
        }

        binding.termEditText.setOnEditorActionListener { termName ->
            if (termName.isNotBlank() && !assignedTermsAdapter.containsTerm(termName)) {
                addTerm(termName)
                binding.termEditText.text = ""
            }
            true
        }

        setupTabletSecondPaneToolbar(
            title = attributeName,
            onMenuItemSelected = ::onMenuItemSelected,
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onBackButtonClicked(ExitProductAddAttributeTerms)
                }
                onCreateMenu(toolbar)
            }
        )
    }

    private fun initializeRecycler(
        recycler: RecyclerView,
        enableDragAndDrop: Boolean,
        enableDeleting: Boolean,
        supportsLoadMore: Boolean
    ): LinearLayoutManager {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        recycler.layoutManager = layoutManager

        with(TypedValue()) {
            context?.theme?.resolveAttribute(android.R.attr.itemBackground, this, true)

            if (enableDragAndDrop) {
                recycler.adapter = AttributeTermsListAdapter(
                    enableDragAndDrop = true,
                    enableDeleting = enableDeleting,
                    defaultItemBackground = this,
                    loadMoreListener = (::onLoadMoreRequested).takeIf { supportsLoadMore }
                )
                itemTouchHelper.attachToRecyclerView(recycler)
            } else {
                recycler.adapter = AttributeTermsListAdapter(
                    enableDragAndDrop = false,
                    enableDeleting = enableDeleting,
                    defaultItemBackground = this,
                    loadMoreListener = (::onLoadMoreRequested).takeIf { supportsLoadMore }
                )
            }
        }

        return layoutManager
    }

    private fun setupObservers() {
        termsViewModel.termsListState.observe(viewLifecycleOwner) {
            showGlobalAttributeTerms(it)
        }

        termsViewModel.loadingState.observe(viewLifecycleOwner) {
            showSkeleton(it == Loading)
            binding.loadMoreProgress.isVisible = it == Appending
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitProductAddAttributeTerms -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
    }

    private fun setupResultHandlers() {
        handleResult<String>(RenameAttributeFragment.KEY_RENAME_ATTRIBUTE_RESULT) {
            // note we always pass 0L as the attributeId since renaming is only supported for local attributes
            if (viewModel.renameAttributeInDraft(0L, oldAttributeName = attributeName, newAttributeName = it)) {
                renamedAttributeName = it
            }
        }
    }

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
        moveNextMenuItem?.isVisible = !assignedTermsAdapter.isEmpty() && navArgs.isVariationCreation
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

        viewModel.addAttributeTermToDraft(navArgs.attributeId, attributeName, termName)
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

    private fun confirmRemoveAttribute() {
        isConfirmRemoveDialogShowing = true
        WooDialog.showDialog(
            requireActivity(),
            messageId = R.string.product_attribute_remove,
            positiveButtonId = R.string.remove,
            posBtnAction = { _, _ ->
                isConfirmRemoveDialogShowing = false
                removeAttribute()
            },
            negativeButtonId = R.string.cancel,
            negBtnAction = { _, _ ->
                isConfirmRemoveDialogShowing = false
            }
        )
    }

    /**
     * Removes this attribute from the product draft and returns to the attributes screen
     */
    private fun removeAttribute() {
        viewModel.removeAttributeFromDraft(navArgs.attributeId, attributeName)
        saveChangesAndReturn()
    }

    private fun onLoadMoreRequested() {
        termsViewModel.onLoadMore(navArgs.attributeId)
    }
}
