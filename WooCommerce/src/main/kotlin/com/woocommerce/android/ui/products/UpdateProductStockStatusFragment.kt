package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdateProductStockStatusFragment : BaseFragment() {
    private val viewModel: UpdateProductStockStatusViewModel by viewModels()
    private val args: UpdateProductStockStatusFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            UpdateProductStockStatusScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadProductStockStatuses(args.selectedProductIds.toList())
    }
}
