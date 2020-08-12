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
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.products.ProductDetailTypesBottomSheetViewModel.ExitWithResult
import com.woocommerce.android.ui.products.ProductDetailTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_product_detail_bottom_sheet_list.*
import javax.inject.Inject

class ProductAddTypesBottomSheetFragment : BottomSheetDialogFragment(), HasAndroidInjector {
    @Inject internal lateinit var navigator: ProductNavigator
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ProductAddTypesBottomSheetViewModel by viewModels { viewModelFactory }

    private val productTypesBottomSheetAdapter: ProductTypesBottomSheetAdapter by lazy {
        ProductTypesBottomSheetAdapter(viewModel::onProductTypeSelected)
    }

    override fun androidInjector(): AndroidInjector<Any> = childInjector

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.dialog_product_detail_bottom_sheet_list, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        AndroidSupportInjection.inject(this)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        viewModel.loadProductTypes()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        productDetailInfo_lblTitle.text = getString(string.product_type_list_header)
        productDetailInfo_optionsList.apply {
            adapter = productTypesBottomSheetAdapter
            layoutManager = LinearLayoutManager(activity)
        }
    }

    private fun setupObservers() {
        viewModel.productTypesBottomSheetList.observe(viewLifecycleOwner, Observer {
            showProductTypeOptions(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ExitWithResult -> {
                    dismiss()
                }
                else -> event.isHandled = false
            }
        })
    }

    private fun showProductTypeOptions(productTypeOptions: List<ProductTypesBottomSheetUiItem>) =
        productTypesBottomSheetAdapter.setProductTypeOptions(productTypeOptions)
}
