package com.woocommerce.android.ui.products

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_product_detail_bottom_sheet_list.*
import javax.inject.Inject

class ProductTypesBottomSheetFragment : BottomSheetDialogFragment(), HasAndroidInjector {
    companion object {
        const val KEY_PRODUCT_TYPE_RESULT = "key_product_type_result"
    }

    @Inject internal lateinit var navigator: ProductNavigator
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ProductTypesBottomSheetViewModel by viewModels { viewModelFactory }

    private lateinit var productTypesBottomSheetAdapter: ProductTypesBottomSheetAdapter

    private val navArgs: ProductTypesBottomSheetFragmentArgs by navArgs()

    override fun androidInjector(): AndroidInjector<Any> {
        return childInjector
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_product_detail_bottom_sheet_list, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        AndroidSupportInjection.inject(this)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val builder = getProductTypeListBuilder()

        viewModel.loadProductTypes(builder = builder)

        productDetailInfo_lblTitle.text = getString(R.string.product_type_list_header)
        productTypesBottomSheetAdapter = ProductTypesBottomSheetAdapter(
            viewModel::onProductTypeSelected
        )
        with(productDetailInfo_optionsList) {
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
                is Exit -> {
                    dismiss()
                }
                is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                    requireActivity(),
                    event.positiveBtnAction,
                    event.negativeBtnAction,
                    event.neutralBtnAction,
                    event.titleId,
                    event.messageId,
                    event.positiveButtonId,
                    event.negativeButtonId,
                    event.neutralButtonId
                )

                is ExitWithResult<*> -> {
                    (event.data as? ProductTypesBottomSheetUiItem)?.let {
                        navigateWithSelectedResult(type = it)
                    }
                }

                is ProductNavigationTarget -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        })
    }

    private fun showProductTypeOptions(
        productTypeOptions: List<ProductTypesBottomSheetUiItem>
    ) {
        productTypesBottomSheetAdapter.setProductTypeOptions(productTypeOptions)
    }

    private fun getProductTypeListBuilder(): ProductTypeBottomSheetBuilder {
        return when (navArgs.isAddProduct) {
            true -> ProductAddTypeBottomSheetBuilder()
            else -> ProductDetailTypeBottomSheetBuilder()
        }
    }

    private fun navigateWithSelectedResult(type: ProductTypesBottomSheetUiItem) {
        when (navArgs.isAddProduct) {
            true -> dismiss()
            else -> navigateBackWithResult(KEY_PRODUCT_TYPE_RESULT, type)
        }
    }
}
