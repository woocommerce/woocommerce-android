package com.woocommerce.android.ui.prefs.domain

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartRestClient.ShoppingCart
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartRestClient.ShoppingCart.CartProduct
import org.wordpress.android.fluxc.network.rest.wpcom.wc.storecreation.ShoppingCartStore
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class DomainChangeRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val siteStore: SiteStore,
    private val shoppingCartStore: ShoppingCartStore
) {
    suspend fun fetchSiteDomains(): Result<List<Domain>> {
        val result = withContext(Dispatchers.Default) {
            siteStore.fetchSiteDomains(selectedSite.get())
        }
        return if (result.isError) {
            Result.failure(Exception(result.error.message))
        } else {
            Result.success(result.domains ?: emptyList())
        }
    }

    suspend fun fetchActiveSitePlan(): Result<PlanModel> {
        val result = withContext(Dispatchers.Default) {
            siteStore.fetchSitePlans(selectedSite.get())
        }
        val plan = result.plans?.firstOrNull { it.isCurrentPlan }
        return when {
            result.isError -> Result.failure(Exception(result.error.message))
            plan == null -> Result.failure(Exception("No active plan found"))
            else -> Result.success(plan)
        }
    }

    suspend fun addDomainToCart(productId: Int, domain: String, isPrivate: Boolean): WooResult<ShoppingCart> {
        val eCommerceProduct = CartProduct(
            productId = productId,
            extra = mapOf("privacy" to isPrivate),
            meta = domain
        )

        return shoppingCartStore.addProductToCart(selectedSite.get().siteId, eCommerceProduct)
    }
}
