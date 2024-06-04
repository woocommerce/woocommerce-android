package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCShippingMethodsStore
import javax.inject.Inject

class ShippingMethodsRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val shippingMethodsStore: WCShippingMethodsStore,
    private val resourceProvider: ResourceProvider,
    private val dispatchers: CoroutineDispatchers
) {
    companion object {
        const val OTHER_ID = "other"
        const val NA_ID = ""
    }

    suspend fun fetchShippingMethodsAndSaveResults(
        site: SiteModel = selectedSite.get()
    ): WooResult<List<ShippingMethod>> {
        return withContext(dispatchers.io) {
            val result = shippingMethodsStore.fetchShippingMethods(site)
            when {
                result.isError -> {
                    WooResult(result.error)
                }

                result.model != null -> {
                    val shippingMethods = result.model!!
                    val mappedValues = shippingMethods.map { it.toAppModel() }
                    shippingMethodsStore.updateShippingMethods(site, shippingMethods)
                    WooResult(mappedValues)
                }

                else -> {
                    WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
                }
            }
        }
    }

    suspend fun fetchShippingMethodByIdAndSaveResult(
        methodId: String,
        site: SiteModel = selectedSite.get()
    ): WooResult<ShippingMethod> {
        return withContext(dispatchers.io) {
            val response = shippingMethodsStore.fetchShippingMethod(site, methodId)
            when {
                response.isError -> {
                    WooResult(response.error)
                }

                response.model != null -> {
                    val shippingMethod = response.model!!
                    shippingMethodsStore.updateShippingMethod(site, shippingMethod)
                    WooResult(shippingMethod.toAppModel())
                }

                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
            }
        }
    }

    fun observeShippingMethods(site: SiteModel = selectedSite.get()) =
        shippingMethodsStore.observeShippingMethods(site).map { list -> list.map { it.toAppModel() } }

    suspend fun getShippingMethodById(
        methodId: String,
        site: SiteModel = selectedSite.get()
    ): ShippingMethod? = shippingMethodsStore.getShippingMethodById(site = site, id = methodId)?.toAppModel()

    fun getOtherShippingMethod(): ShippingMethod {
        return ShippingMethod(
            id = OTHER_ID,
            title = resourceProvider.getString(R.string.other)
        )
    }

    fun getNAShippingMethod(): ShippingMethod {
        return ShippingMethod(
            id = NA_ID,
            title = resourceProvider.getString(R.string.na)
        )
    }
}
