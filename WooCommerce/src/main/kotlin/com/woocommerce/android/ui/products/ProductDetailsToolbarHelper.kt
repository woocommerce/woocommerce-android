package com.woocommerce.android.ui.products

import android.app.Activity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.DefaultLifecycleObserver
import com.woocommerce.android.databinding.FragmentProductDetailBinding
import javax.inject.Inject

class ProductDetailsToolbarHelper @Inject constructor(
    private val activity: Activity,
) : DefaultLifecycleObserver {
    private var fragment: ProductDetailFragment? = null
    private var binding: FragmentProductDetailBinding? = null

    fun onViewCreated(
        fragment: ProductDetailFragment,
        binding: FragmentProductDetailBinding,
    ) {
        this.fragment = fragment
        this.binding = binding

        fragment.lifecycle.addObserver(this)

        setupToolbar(binding.productDetailToolbar)
    }

    fun updateTitle(title: String) {
        binding?.productDetailToolbar?.title = title
    }

    private fun setupToolbar(toolbar: Toolbar) {
    }
}
