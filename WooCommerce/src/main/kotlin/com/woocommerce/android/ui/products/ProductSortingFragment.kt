package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.databinding.DialogProductListSortingBinding
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductSortingFragment : WCBottomSheetDialogFragment() {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: ProductSortingViewModel by viewModels()

    private var _binding: DialogProductListSortingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogProductListSortingBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        showSortingOptions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers() {
        viewModel.event.observe(
            viewLifecycleOwner,
            Observer { event ->
                when (event) {
                    is Exit -> {
                        dismiss()
                    }
                    else -> event.isHandled = false
                }
            }
        )
    }

    private fun showSortingOptions() {
        val adapter = binding.sortingOptionsList.adapter as? ProductSortingListAdapter
            ?: ProductSortingListAdapter(
                viewModel::onSortingOptionChanged,
                ProductSortingViewModel.SORTING_OPTIONS,
                viewModel.sortingChoice
            )
        binding.sortingOptionsList.adapter = adapter
        binding.sortingOptionsList.layoutManager = LinearLayoutManager(activity)
    }
}
