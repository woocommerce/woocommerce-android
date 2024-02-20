package com.woocommerce.android.ui.products

import android.app.Activity
import androidx.annotation.IdRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
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
        toolbar.navigationIcon = if (fragment?.findNavController()?.hasBackStackEntry(R.id.products) == true) {
            AppCompatResources.getDrawable(activity, R.drawable.ic_back_24dp)
        } else {
            AppCompatResources.getDrawable(activity, R.drawable.ic_gridicons_cross_24dp)
        }
    }

    private fun NavController.hasBackStackEntry(@IdRes destinationId: Int) = runCatching {
        getBackStackEntry(destinationId)
    }.isSuccess
}
