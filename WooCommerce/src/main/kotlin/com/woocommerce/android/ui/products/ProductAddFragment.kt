package com.woocommerce.android.ui.products

import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R

class ProductAddFragment : BaseProductFragment() {
    val productAddViewModel: ProductAddViewModel by navGraphViewModels(R.id.nav_graph_products) { viewModelFactory }

    private fun initializeViewModel() {
        // todo
    }

    private fun setupObservers() {
        // todo
    }

    override fun onRequestAllowBackPress(): Boolean {
        // todo
        return true
    }
}

