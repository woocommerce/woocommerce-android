package com.woocommerce.android.ui.products.variations.attributes.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentEditVariationAttributesBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.variations.attributes.edit.EditVariationAttributesViewModel.ViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import javax.inject.Inject

class EditVariationAttributesFragment :
    BaseFragment(R.layout.fragment_edit_variation_attributes), BackPressListener {
    companion object {
        const val TAG: String = "EditVariationAttributesFragment"
        const val KEY_VARIATION_ATTRIBUTES_RESULT = "key_variation_attributes_result"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: EditVariationAttributesViewModel by activityViewModels { viewModelFactory }

    private val navArgs: EditVariationAttributesFragmentArgs by navArgs()

    private lateinit var binding: FragmentEditVariationAttributesBinding

    private val skeletonView = SkeletonView()

    private var skeletonVisibility: Boolean = false
        set(show) {
            field = show
            if (show) skeletonView.show(
                viewActual = binding.attributeSelectionGroupList,
                layoutId = R.layout.skeleton_variation_attributes_list,
                delayed = true
            )
            else skeletonView.hide()
        }

    private val adapter
        get() = binding.attributeSelectionGroupList.adapter as? VariationAttributesAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditVariationAttributesBinding.bind(view)
        setupObservers()
        setupViews()
        viewModel.start(navArgs.remoteProductId, navArgs.remoteVariationId)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return (viewModel.event.value == Exit)
            .also { viewModel.exit() }
    }

    override fun getFragmentTitle() = getString(R.string.product_attributes)

    private fun setupObservers() = viewModel.apply {
        viewStateLiveData.observe(viewLifecycleOwner, ::handleViewStateChanges)
        editableVariationAttributeList.observe(viewLifecycleOwner, Observer(::handleVariationAttributeListChanges))
        event.observe(viewLifecycleOwner, Observer(::handleViewModelEvents))
    }

    private fun setupViews() = binding.apply {
        attributeSelectionGroupList.apply {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            itemAnimator = null
        }
    }

    private fun handleViewStateChanges(old: ViewState?, new: ViewState?) {
        new?.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { skeletonVisibility = it }
    }

    private fun handleVariationAttributeListChanges(selectableOptions: List<VariationAttributeSelectionGroup>) {
        showAttributeSelectableOptions(selectableOptions.toMutableList())
        requireActivity().invalidateOptionsMenu()
    }

    private fun handleViewModelEvents(event: MultiLiveEvent.Event) {
        when (event) {
            is ExitWithResult<*> -> navigateBackWithResult(KEY_VARIATION_ATTRIBUTES_RESULT, event.data)
            is Exit -> findNavController().navigateUp()
            else -> event.isHandled = false
        }
    }

    private fun showAttributeSelectableOptions(
        selectableOptions: MutableList<VariationAttributeSelectionGroup>
    ) {
        adapter?.refreshSourceData(selectableOptions)
            ?: binding.attributeSelectionGroupList.apply {
                adapter = VariationAttributesAdapter(
                    selectableOptions,
                    ::displaySelectionDialog
                )
            }
    }

    private fun displaySelectionDialog(item: VariationAttributeSelectionGroup) {
        AttributeOptionSelectorDialog.newInstance(
            attributeGroup = item,
            onAttributeOptionSelected = { modifiedGroup ->
                adapter?.apply {
                    refreshSingleAttributeSelectionGroup(modifiedGroup)
                    viewModel.updateData(sourceData)
                }
            }
        ).also { it.show(parentFragmentManager, AttributeOptionSelectorDialog.TAG) }
    }
}
