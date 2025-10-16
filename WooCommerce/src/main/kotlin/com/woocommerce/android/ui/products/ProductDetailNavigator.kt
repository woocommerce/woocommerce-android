package com.woocommerce.android.ui.products

import android.view.View
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.navOptions
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

class ProductDetailNavigator @Inject constructor(
    private val activity: MainActivity,
    private val selectedSite: SelectedSite,
    private val productDetailRepository: ProductDetailRepository
) {
    fun showProductDetail(
        remoteProductId: Long,
        popUpToProductList: Boolean = false,
        sharedView: View? = null
    ) {
        showProductDetailFragment(
            remoteProductId = remoteProductId,
            popUpToProductList = popUpToProductList,
            sharedView = sharedView
        )
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

    private fun showProductInWebView(product: Product, popUpToProductList: Boolean) {
        // TODO replace with final URL when confirmed
        fun buildProductUrl(): String {
            val site = selectedSite.get()
            return site.adminUrlOrDefault.slashJoin("?page=next-admin&p=/woocommerce/products/edit/${product.remoteId}")
        }

        val action = NavGraphMainDirections.actionGlobalAuthenticatedWebViewFragment(
            urlToLoad = buildProductUrl(),
            title = product.name
        )
        activity.findNavController(R.id.nav_host_fragment_main).navigateSafely(
            directions = action,
            navOptions = navOptions {
                if (popUpToProductList) {
                    popUpTo(R.id.products)
                }
            }
        )
    }
}
