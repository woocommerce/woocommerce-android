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
import com.woocommerce.android.databinding.DialogProductDetailBottomSheetListBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
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

    private var _binding: DialogProductDetailBottomSheetListBinding? = null
    private val binding get() = _binding!!

    override fun androidInjector(): AndroidInjector<Any> {
        return childInjector
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogProductDetailBottomSheetListBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        AndroidSupportInjection.inject(this)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        val builder = getProductTypeListBuilder()

        viewModel.loadProductTypes(builder = builder)

        binding.productDetailInfoLblTitle.text = getString(R.string.product_type_list_header)
        productTypesBottomSheetAdapter = ProductTypesBottomSheetAdapter(
            viewModel::onProductTypeSelected
        )
        with(binding.productDetailInfoOptionsList) {
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
                is ShowDialog -> WooDialog.showDialog(
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
                    (event.data as? ProductType)?.let {
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

    private fun navigateWithSelectedResult(type: ProductType) {
        when (navArgs.isAddProduct) {
            true -> dismiss()
            else -> navigateBackWithResult(KEY_PRODUCT_TYPE_RESULT, type)
        }
    }
}
