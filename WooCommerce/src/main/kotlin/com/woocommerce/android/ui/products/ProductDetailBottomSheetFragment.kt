package com.woocommerce.android.ui.products

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogProductDetailBottomSheetListBinding
import com.woocommerce.android.ui.products.ProductDetailBottomSheetBuilder.ProductDetailBottomSheetUiItem
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class ProductDetailBottomSheetFragment : BottomSheetDialogFragment(), HasAndroidInjector {
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ProductDetailViewModel by navGraphViewModels(R.id.nav_graph_products) { viewModelFactory }

    private lateinit var productDetailBottomSheetAdapter: ProductDetailBottomSheetAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        AndroidSupportInjection.inject(this)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return DialogProductDetailBottomSheetListBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = DialogProductDetailBottomSheetListBinding.bind(view)

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
        viewModel.fetchBottomSheetList()
    }

    private fun setupObservers() {
        viewModel.productDetailBottomSheetList.observe(viewLifecycleOwner, Observer {
            showProductDetailBottomSheetOptions(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is Exit -> {
                    dismiss()
                }
                else -> event.isHandled = false
            }
        })
    }

    private fun showProductDetailBottomSheetOptions(
        productDetailBottomSheetOptions: List<ProductDetailBottomSheetUiItem>
    ) {
        productDetailBottomSheetAdapter.setProductDetailBottomSheetOptions(productDetailBottomSheetOptions)
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return childInjector
    }
}
