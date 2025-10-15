package com.woocommerce.android.ui.products

import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.navOptions
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class ProductDetailNavigator @Inject constructor(
    private val activity: MainActivity,
    private val productDetailRepository: ProductDetailRepository
) {
    fun showProductDetail(
        remoteProductId: Long,
        popUpToProductList: Boolean = false,
        sharedView: View? = null
    ) {
        activity.lifecycleScope.launch {
            val product = productDetailRepository.getProductAsync(remoteProductId)

            if (false /* product?.productType = ProductType.Booking */) {
                TODO("Open in a WebView")
            } else {
                showProductDetailFragment(
                    remoteProductId = remoteProductId,
                    popUpToProductList = popUpToProductList,
                    sharedView = sharedView
                )
            }
        }
    }

    private fun showProductDetailFragment(
        remoteProductId: Long,
        popUpToProductList: Boolean = false,
        sharedView: View? = null
    ) {
        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(
            mode = ProductDetailFragment.Mode.ShowProduct(remoteProductId),
        )
        val extras = if (sharedView != null) {
            val productCardDetailTransitionName = activity.getString(R.string.product_card_detail_transition_name)
            FragmentNavigatorExtras(sharedView to productCardDetailTransitionName)
        } else {
            null
        }
        activity.findNavController(R.id.nav_host_fragment_main).navigateSafely(
            directions = action,
            extras = extras,
            navOptions = navOptions {
                if (popUpToProductList) {
                    popUpTo(R.id.products)
                }
            }
        )
    }
}
