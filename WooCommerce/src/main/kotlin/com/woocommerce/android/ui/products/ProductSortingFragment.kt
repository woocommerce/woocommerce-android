package com.woocommerce.android.ui.products

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.databinding.DialogProductListSortingBinding
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class ProductSortingFragment : BottomSheetDialogFragment(), HasAndroidInjector {
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: ProductSortingViewModel by viewModels { viewModelFactory }

    private var _binding: DialogProductListSortingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogProductListSortingBinding.inflate(inflater)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        AndroidSupportInjection.inject(this)
        return super.onCreateDialog(savedInstanceState)
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
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is Exit -> {
                    dismiss()
                }
                else -> event.isHandled = false
            }
        })
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

    override fun androidInjector(): AndroidInjector<Any> {
        return childInjector
    }
}
