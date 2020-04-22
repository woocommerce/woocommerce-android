package com.woocommerce.android.ui.products

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.ProductSortingViewModel.SortingListItemUIModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_product_list_sorting.*
import javax.inject.Inject

class ProductSortingFragment : BottomSheetDialogFragment(), HasAndroidInjector {
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: ProductSortingViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_product_list_sorting, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        AndroidSupportInjection.inject(this)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.productSortingViewState.observe(viewLifecycleOwner) { old, new ->
            new.sortingOptions?.takeIfNotEqualTo(old?.sortingOptions) {
                showSortingOptions(it)
            }
        }
    }

    private fun showSortingOptions(options: List<SortingListItemUIModel>) {
        val adapter = sorting_optionsList.adapter as? ProductSortingListAdapter
                ?: ProductSortingListAdapter(viewModel::onSortingOptionChanged)
        sorting_optionsList.adapter = adapter
        sorting_optionsList.layoutManager = LinearLayoutManager(activity)
        adapter.update(options)
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return childInjector
    }
}
