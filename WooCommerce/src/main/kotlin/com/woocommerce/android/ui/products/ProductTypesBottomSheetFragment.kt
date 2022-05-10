package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogProductDetailBottomSheetListBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductTypesBottomSheetFragment : WCBottomSheetDialogFragment() {
    companion object {
        const val KEY_PRODUCT_TYPE_RESULT = "key_product_type_result"
    }

    @Inject internal lateinit var navigator: ProductNavigator
    val viewModel: ProductTypesBottomSheetViewModel by viewModels()

    private lateinit var productTypesBottomSheetAdapter: ProductTypesBottomSheetAdapter

    private val navArgs: ProductTypesBottomSheetFragmentArgs by navArgs()

    private var _binding: DialogProductDetailBottomSheetListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogProductDetailBottomSheetListBinding.inflate(inflater)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        viewModel.loadProductTypes()

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
        viewModel.productTypesBottomSheetList.observe(
            viewLifecycleOwner,
            {
                showProductTypeOptions(it)
            }
        )

        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
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
                        (event.data as? ProductTypesBottomSheetUiItem)?.let {
                            navigateWithSelectedResult(productTypesBottomSheetUiItem = it)
                        }
                    }

                    is ProductNavigationTarget -> navigator.navigate(this, event)
                    else -> event.isHandled = false
                }
            }
        )
    }

    private fun showProductTypeOptions(
        productTypeOptions: List<ProductTypesBottomSheetUiItem>
    ) {
        productTypesBottomSheetAdapter.setProductTypeOptions(productTypeOptions)
    }

    private fun navigateWithSelectedResult(productTypesBottomSheetUiItem: ProductTypesBottomSheetUiItem) {
        when (navArgs.isAddProduct) {
            true -> dismiss()
            else -> navigateBackWithResult(KEY_PRODUCT_TYPE_RESULT, productTypesBottomSheetUiItem)
        }
    }
}
