package com.woocommerce.android.ui.products

import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment

/**
 * All product related fragments should extend this class to provide a consistent method
 * of fetching and updating product
 */
abstract class BaseProductFragment : BaseFragment(), BaseProductFragmentView {
    open fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.commonStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.product?.takeIfNotEqualTo(old?.product) { updateProductView(new) }
            new.isProductUpdated?.takeIfNotEqualTo(old?.isProductUpdated) { showUpdateProductAction(it) }
        }
    }
}
