package com.woocommerce.android.ui.products.variations.attributes

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentAttributesAddedBinding
import com.woocommerce.android.ui.base.BaseFragment

class AttributesAddedFragment : BaseFragment(R.layout.fragment_attributes_added) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FragmentAttributesAddedBinding.bind(view).apply {
            generateVariationButton.setOnClickListener(::onGenerateVariationClicked)
        }
    }

    private fun onGenerateVariationClicked(view: View) {

    }
}
