package com.woocommerce.android.ui.products

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_product_add_type_bottom_sheet_list.*
import javax.inject.Inject

class ProductAddTypeBottomSheetFragment : BottomSheetDialogFragment(), HasAndroidInjector {
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ProductAddTypeBottomViewModel by navGraphViewModels(R.id.nav_graph_products) { viewModelFactory }

    private val productAddTypeBottomSheetAdapter: ProductAddTypeBottomSheetAdapter by lazy {
        ProductAddTypeBottomSheetAdapter(viewModel::onProductSelected)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_product_add_type_bottom_sheet_list, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        AndroidSupportInjection.inject(this)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        showProductAddTypesBottomSheetOptions(viewModel.getProductTypesList())
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is Exit -> dismiss()
                else -> event.isHandled = false
            }
        })
    }

    private fun showProductAddTypesBottomSheetOptions(items: List<ProductTypesBottomSheetUiItem>) {
        productAddTypeBottomSheetAdapter.setProductTypeOptions(items = items)
    }

    override fun androidInjector(): AndroidInjector<Any> = childInjector
}
