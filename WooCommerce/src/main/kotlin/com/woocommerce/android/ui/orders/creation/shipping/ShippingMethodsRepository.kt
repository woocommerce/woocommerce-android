package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.network.shippingmethods.ShippingMethodsRestClient
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import javax.inject.Inject

class ShippingMethodsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val shippingMethodsRestClient: ShippingMethodsRestClient,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: CoroutineDispatchers
) {
    companion object {
        const val OTHER_ID = "other"
    }

    suspend fun fetchShippingMethods(site: SiteModel = selectedSite.get()): WooResult<List<ShippingMethod>> {
        return withContext(dispatchers.io) {
            val response = shippingMethodsRestClient.fetchShippingMethods(site)
            when {
                response.isError -> {
                    WooResult(response.error)
                }

                response.result != null -> {
                    val shippingMethods = response.result!!.map { dto -> dto.toAppModel() }
                    WooResult(shippingMethods)
                }

                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
            }
        }
    }

    suspend fun fetchShippingMethodById(
        methodId: String,
        site: SiteModel = selectedSite.get()
    ): WooResult<ShippingMethod> {
        return withContext(dispatchers.io) {
            val response = shippingMethodsRestClient.fetchShippingMethodsById(site, methodId)
            when {
                response.isError -> {
                    WooResult(response.error)
                }

                response.result != null -> {
                    WooResult(response.result!!.toAppModel())
                }

                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
            }
        }
    }

    fun getOtherShippingMethod(): ShippingMethod {
        return ShippingMethod(
            id = OTHER_ID,
            title = resourceProvider.getString(R.string.other)
        )
    }
}
