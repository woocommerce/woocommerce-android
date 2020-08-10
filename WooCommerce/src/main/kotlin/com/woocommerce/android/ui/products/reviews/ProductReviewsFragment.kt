package com.woocommerce.android.ui.products.reviews

import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class ProductReviewsFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ProductReviewsViewModel by viewModels { viewModelFactory }
}
