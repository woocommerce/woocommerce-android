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
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAddAttributeOptionsBinding
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAddAttributeOptions
import com.woocommerce.android.widgets.AlignedDividerDecoration

class AddAttributeOptionsFragment : BaseProductFragment(R.layout.fragment_add_attribute_options) {
    companion object {
        const val TAG: String = "AddAttributeOptionsFragment"
        private const val LIST_STATE_KEY = "list_state"
    }

    private var layoutManager: LayoutManager? = null

    private var _binding: FragmentAddAttributeOptionsBinding? = null
    private val binding get() = _binding!!

    private val navArgs: AddAttributeOptionsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAddAttributeOptionsBinding.bind(view)

        setHasOptionsMenu(true)
        initializeViews(savedInstanceState)
        setupObservers()

        showAvailableAttributeOptions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress() =
        viewModel.onBackButtonClicked(ExitProductAddAttributeOptions())

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
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitProductAddAttributeOptions -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
    }

    override fun getFragmentTitle() = navArgs.attributeName

    private fun showAvailableAttributeOptions() {
        val adapter: AttributeOptionListAdapter
        if (binding.attributeList.adapter == null) {
            adapter = AttributeOptionListAdapter()
            binding.attributeList.adapter = adapter
        } else {
            adapter = binding.attributeList.adapter as AttributeOptionListAdapter
        }

        val options = viewModel.getProductDraftAttributeOptions(navArgs.attributeId, navArgs.attributeName)
        adapter.setOptionList(options)
    }
}
