package com.woocommerce.android.ui.products.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogProductDetailBottomSheetListBinding
import com.woocommerce.android.ui.products.details.ProductDetailBottomSheetBuilder.ProductDetailBottomSheetUiItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProductDetailBottomSheetFragment : WCBottomSheetDialogFragment() {
    val viewModel: ProductDetailViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_products)

    private lateinit var productDetailBottomSheetAdapter: ProductDetailBottomSheetAdapter

    private var _binding: DialogProductDetailBottomSheetListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogProductDetailBottomSheetListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productDetailBottomSheetAdapter = ProductDetailBottomSheetAdapter {
            // Navigate up before navigating to the next destination, this is useful if the next destination
            // is a dialog too
            findNavController().navigateUp()
            viewModel.onProductDetailBottomSheetItemSelected(it)
        }
        with(binding.productDetailInfoOptionsList) {
            adapter = productDetailBottomSheetAdapter
            layoutManager = LinearLayoutManager(activity)
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.productDetailBottomSheetList.observe(viewLifecycleOwner) {
            showProductDetailBottomSheetOptions(it)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> {
                    dismiss()
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun showProductDetailBottomSheetOptions(
        productDetailBottomSheetOptions: List<ProductDetailBottomSheetUiItem>
    ) {
        productDetailBottomSheetAdapter.options = productDetailBottomSheetOptions
    }
}
