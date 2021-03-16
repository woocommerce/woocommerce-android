package com.woocommerce.android.ui.products.variations.attributes

import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.ViewModelFactory
import javax.inject.Inject

class EditVariationAttributesFragment :
    BaseFragment(R.layout.fragment_edit_variation_attributes) {
    companion object {
        const val TAG: String = "EditVariationAttributesFragment"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: EditVariationAttributesViewModel by viewModels { viewModelFactory }
}
