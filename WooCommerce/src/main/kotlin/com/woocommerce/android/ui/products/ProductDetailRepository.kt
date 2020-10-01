package com.woocommerce.android.ui.products

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_LOADED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_UPDATE_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_UPDATE_SUCCESS
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import com.woocommerce.android.util.suspendCancellableCoroutineWithTimeout
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.ADDED_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_PASSWORD
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_SKU_AVAILABILITY
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT_SHIPPING_CLASS
import org.wordpress.android.fluxc.action.WCProductAction.UPDATED_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.UPDATE_PRODUCT_PASSWORD
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductSkuAvailabilityPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCreated
import org.wordpress.android.fluxc.store.WCProductStore.OnProductPasswordChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductShippingClassesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductSkuAvailabilityChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductUpdated
import org.wordpress.android.fluxc.store.WCTaxStore
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@OpenClassOnDebug
class ProductDetailRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val taxStore: WCTaxStore
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var continuationUpdateProduct: Continuation<Boolean>? = null
    private var continuationFetchProduct: CancellableContinuation<Boolean>? = null
    private var continuationFetchProductPassword: CancellableContinuation<String?>? = null
    private var continuationUpdateProductPassword: CancellableContinuation<Boolean>? = null
    private var continuationFetchProductShippingClass: CancellableContinuation<Boolean>? = null
    private var continuationVerifySku: CancellableContinuation<Boolean>? = null

    private var continuationAddProduct: Continuation<Pair<Boolean, Long>>? = null

    private var isFetchingTaxClassList = false
    private var remoteProductId: Long = 0L

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchProduct(remoteProductId: Long): Product? {
        try {
            this.remoteProductId = remoteProductId
            continuationFetchProduct?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchProduct = it

                val payload = WCProductStore.FetchSingleProductPayload(selectedSite.get(), remoteProductId)
                dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "CancellationException while fetching single product")
        }

        continuationFetchProduct = null
        return getProduct(remoteProductId)
    }

    suspend fun fetchProductPassword(remoteProductId: Long): String? {
        var password: String? = null
        try {
            continuationFetchProductPassword?.cancel()
            password = suspendCancellableCoroutineWithTimeout<String?>(ACTION_TIMEOUT) {
                continuationFetchProductPassword = it

                val payload = WCProductStore.FetchProductPasswordPayload(selectedSite.get(), remoteProductId)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductPasswordAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "CancellationException while fetching single product")
        }

        continuationFetchProductPassword = null
        return password
    }

    /**
     * Fires the request to update the product
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun updateProduct(updatedProduct: Product): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationUpdateProduct = it

                val product = updatedProduct.toDataModel(getCachedWCProductModel(updatedProduct.remoteId))
                val payload = WCProductStore.UpdateProductPayload(selectedSite.get(), product)
                dispatcher.dispatch(WCProductActionBuilder.newUpdateProductAction(payload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "Exception encountered while updating product", e)
            false
        }
    }

    /**
     * Fires the request to add a product
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun addProduct(product: Product): Pair<Boolean, Long> {
        return try {
            suspendCoroutineWithTimeout<Pair<Boolean, Long>>(ACTION_TIMEOUT) {
                continuationAddProduct = it
                val model = product.toDataModel(null)
                val payload = WCProductStore.AddProductPayload(selectedSite.get(), model)
                dispatcher.dispatch(WCProductActionBuilder.newAddProductAction(payload))
            } ?: Pair(false, 0L) // request timed out
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "Exception encountered while publishing a product", e)
            Pair(false, 0L)
        }
    }

    /**
     * Fires the request to update the product password
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun updateProductPassword(remoteProductId: Long, password: String?): Boolean {
        return try {
            continuationUpdateProductPassword?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationUpdateProductPassword = it

                val payload = WCProductStore.UpdateProductPasswordPayload(
                        selectedSite.get(),
                        remoteProductId,
                        password ?: ""
                )
                dispatcher.dispatch(WCProductActionBuilder.newUpdateProductPasswordAction(payload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "Exception encountered while updating product password", e)
            false
        }
    }

    /**
     * Fires the request to check if sku is available for a given [selectedSite]
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun isSkuAvailableRemotely(sku: String): Boolean? {
        continuationVerifySku?.cancel()
        return try {
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationVerifySku = it

                val payload = FetchProductSkuAvailabilityPayload(selectedSite.get(), sku)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductSkuAvailabilityAction(payload))
            } // request timed out
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "Exception encountered while verifying product sku availability", e)
            null
        }
    }

    /**
     * Fires the request to fetch the product shipping class for a given [selectedSite] and [remoteShippingClassId]
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun fetchProductShippingClassById(remoteShippingClassId: Long): ShippingClass? {
        try {
            continuationFetchProductShippingClass?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchProduct = it

                val payload = WCProductStore.FetchSingleProductShippingClassPayload(
                        selectedSite.get(), remoteShippingClassId
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductShippingClassAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.d(PRODUCTS, "CancellationException while fetching single product shipping class")
        }

        continuationFetchProductShippingClass = null
        return getProductShippingClassByRemoteId(remoteShippingClassId)
    }

    /**
     * Fires the request to fetch list of [TaxClass] for a given [selectedSite]
     *
     * @return the result of the action as a [RequestResult]
     */
    suspend fun loadTaxClassesForSite(): RequestResult {
        return withContext(Dispatchers.IO) {
            if (!isFetchingTaxClassList) {
                isFetchingTaxClassList = true
                val result = taxStore.fetchTaxClassList(selectedSite.get())
                isFetchingTaxClassList = false
                if (result.isError) {
                    WooLog.e(PRODUCTS, "Exception encountered while fetching tax class list: ${result.error.message}")
                    RequestResult.ERROR
                } else RequestResult.SUCCESS
            } else RequestResult.NO_ACTION_NEEDED
        }
    }

    private fun getCachedWCProductModel(remoteProductId: Long) =
            productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)

    fun getProduct(remoteProductId: Long): Product? = getCachedWCProductModel(remoteProductId)?.toAppModel()

    fun isSkuAvailableLocally(sku: String) = !productStore.geProductExistsBySku(selectedSite.get(), sku)

    fun getCachedVariationCount(remoteProductId: Long) =
            productStore.getVariationsForProduct(selectedSite.get(), remoteProductId).size

    fun getTaxClassesForSite(): List<TaxClass> =
            taxStore.getTaxClassListForSite(selectedSite.get()).map { it.toAppModel() }

    /**
     * Returns the cached (SQLite) shipping class for the given [remoteShippingClassId]
     */
    fun getProductShippingClassByRemoteId(remoteShippingClassId: Long) =
            productStore.getShippingClassByRemoteId(selectedSite.get(), remoteShippingClassId)?.toAppModel()

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_SINGLE_PRODUCT && event.remoteProductId == remoteProductId) {
            if (continuationFetchProduct?.isActive == true) {
                if (event.isError) {
                    continuationFetchProduct?.resume(false)
                } else {
                    AnalyticsTracker.track(PRODUCT_DETAIL_LOADED)
                    continuationFetchProduct?.resume(true)
                }
            } else {
                WooLog.w(PRODUCTS, "continuationFetchProduct is no longer active")
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductPasswordChanged(event: OnProductPasswordChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_PASSWORD && event.remoteProductId == remoteProductId) {
            if (continuationFetchProductPassword?.isActive == true) {
                if (event.isError) {
                    continuationFetchProductPassword?.resume(null)
                } else {
                    continuationFetchProductPassword?.resume(event.password)
                }
            } else {
                WooLog.w(PRODUCTS, "continuationFetchProductPassword is no longer active")
            }
        } else if (event.causeOfChange == UPDATE_PRODUCT_PASSWORD && event.remoteProductId == remoteProductId) {
            if (continuationUpdateProductPassword?.isActive == true) {
                if (event.isError) {
                    continuationUpdateProductPassword?.resume(false)
                } else {
                    continuationUpdateProductPassword?.resume(true)
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductUpdated(event: OnProductUpdated) {
        if (event.causeOfChange == UPDATED_PRODUCT) {
            if (event.isError) {
                AnalyticsTracker.track(PRODUCT_DETAIL_UPDATE_ERROR, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))
                continuationUpdateProduct?.resume(false)
            } else {
                AnalyticsTracker.track(PRODUCT_DETAIL_UPDATE_SUCCESS)
                continuationUpdateProduct?.resume(true)
            }
            continuationUpdateProduct = null
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductSkuAvailabilityChanged(event: OnProductSkuAvailabilityChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_SKU_AVAILABILITY) {
            // TODO: add event to track sku availability success
            continuationVerifySku?.resume(event.available)
            continuationVerifySku = null
        }
    }

    /**
     * The shipping class for the product has been fetched
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductShippingClassesChanged(event: OnProductShippingClassesChanged) {
        if (event.causeOfChange == FETCH_SINGLE_PRODUCT_SHIPPING_CLASS) {
            if (event.isError) {
                continuationFetchProductShippingClass?.resume(false)
            } else {
                continuationFetchProductShippingClass?.resume(true)
            }
        }
    }

    /**
     * A new product has been added
     */

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductCreated(event: OnProductCreated) {
        if (event.causeOfChange == ADDED_PRODUCT) {
            if (event.isError) {
                val pair = Pair(false, 0L)
                continuationAddProduct?.resume(pair)
            } else {
                val pair = Pair(true, event.remoteProductId)
                continuationAddProduct?.resume(pair)
            }
            continuationAddProduct = null
        }
    }
}
