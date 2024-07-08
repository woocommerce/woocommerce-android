package com.woocommerce.android.ui.products.details

import com.google.gson.Gson
import com.woocommerce.android.AppConstants
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DETAIL_UPDATE_ERROR
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_DETAIL_UPDATE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.model.ProductGlobalAttribute
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.models.QuantityRules
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.ADDED_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_PASSWORD
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_SKU_AVAILABILITY
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT_SHIPPING_CLASS
import org.wordpress.android.fluxc.action.WCProductAction.UPDATED_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.UPDATE_PRODUCT_PASSWORD
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.store.WCGlobalAttributeStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductSkuAvailabilityPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCreated
import org.wordpress.android.fluxc.store.WCProductStore.OnProductPasswordChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductShippingClassesChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductSkuAvailabilityChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductUpdated
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import org.wordpress.android.fluxc.store.WCTaxStore
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ProductDetailRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val globalAttributeStore: WCGlobalAttributeStore,
    private val selectedSite: SelectedSite,
    private val taxStore: WCTaxStore,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val gson: Gson
) {
    private var continuationUpdateProduct: Continuation<Pair<Boolean, WCProductStore.ProductError?>>? = null
    private var continuationFetchProductPassword = ContinuationWrapper<String?>(PRODUCTS)
    private var continuationUpdateProductPassword = ContinuationWrapper<Boolean>(PRODUCTS)
    private var continuationFetchProductShippingClass = ContinuationWrapper<Boolean>(PRODUCTS)
    private var continuationVerifySku = ContinuationWrapper<Boolean>(PRODUCTS)

    private var continuationAddProduct: Continuation<Pair<Boolean, Long>>? = null

    private var isFetchingTaxClassList = false
    private var remoteProductId: Long = 0L

    var lastFetchProductErrorType: ProductErrorType? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchProductOrLoadFromCache(remoteProductId: Long): Product? {
        val payload = WCProductStore.FetchSingleProductPayload(selectedSite.get(), remoteProductId)
        val result = productStore.fetchSingleProduct(payload)

        if (result.isError) {
            lastFetchProductErrorType = result.error.type
        }

        return getProduct(remoteProductId)
    }

    suspend fun fetchProductPassword(remoteProductId: Long): String? {
        this.remoteProductId = remoteProductId
        val result = continuationFetchProductPassword.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.FetchProductPasswordPayload(selectedSite.get(), remoteProductId)
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductPasswordAction(payload))
        }
        return when (result) {
            is Cancellation -> null
            is Success -> result.value
        }
    }

    /**
     * Fires the request to update the product
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun updateProduct(updatedProduct: Product): Pair<Boolean, WCProductStore.ProductError?> {
        return try {
            suspendCoroutineWithTimeout<Pair<Boolean, WCProductStore.ProductError?>>(AppConstants.REQUEST_TIMEOUT) {
                continuationUpdateProduct = it

                val cachedProduct = getCachedWCProductModel(updatedProduct.remoteId)
                val product = updatedProduct.toDataModel(cachedProduct)
                val payload = WCProductStore.UpdateProductPayload(selectedSite.get(), product)
                dispatcher.dispatch(WCProductActionBuilder.newUpdateProductAction(payload))
            } ?: Pair(false, null) // request timed out
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "Exception encountered while updating product", e)
            Pair(false, null)
        }
    }

    /**
     * Fires the request to add a product
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun addProduct(product: Product): Pair<Boolean, Long> {
        return try {
            suspendCoroutineWithTimeout<Pair<Boolean, Long>>(AppConstants.REQUEST_TIMEOUT) {
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
        this.remoteProductId = remoteProductId
        val result = continuationUpdateProductPassword.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.UpdateProductPasswordPayload(
                selectedSite.get(),
                remoteProductId,
                password ?: ""
            )
            dispatcher.dispatch(WCProductActionBuilder.newUpdateProductPasswordAction(payload))
        }

        return when (result) {
            is Cancellation -> false
            is Success -> result.value
        }
    }

    /**
     * Fires the request to check if sku is available for a given [selectedSite]
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun isSkuAvailableRemotely(sku: String): Boolean? {
        val result = continuationVerifySku.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = FetchProductSkuAvailabilityPayload(selectedSite.get(), sku)
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductSkuAvailabilityAction(payload))
        }
        return when (result) {
            is Cancellation -> null
            is Success -> result.value
        }
    }

    /**
     * Fires the request to fetch the product shipping class for a given [selectedSite] and [remoteShippingClassId]
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun fetchProductShippingClassById(remoteShippingClassId: Long): ShippingClass? {
        continuationFetchProductShippingClass.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.FetchSingleProductShippingClassPayload(
                selectedSite.get(),
                remoteShippingClassId
            )
            dispatcher.dispatch(WCProductActionBuilder.newFetchSingleProductShippingClassAction(payload))
        }
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
                    WooLog.e(
                        PRODUCTS,
                        "Exception encountered while fetching tax class list: ${result.error.message}"
                    )
                    RequestResult.ERROR
                } else {
                    RequestResult.SUCCESS
                }
            } else {
                RequestResult.NO_ACTION_NEEDED
            }
        }
    }

    /**
     * Fetches the list of store-wide attributes
     */
    suspend fun fetchGlobalAttributes(): List<ProductGlobalAttribute> {
        val wooResult = globalAttributeStore.fetchStoreAttributes(selectedSite.get())
        return wooResult.model?.map { it.toAppModel() } ?: emptyList()
    }

    /**
     * Fetches the list of terms for an attribute
     */
    suspend fun fetchGlobalAttributeTerms(
        remoteAttributeId: Long,
        page: Int,
        pageSize: Int
    ): List<ProductAttributeTerm> {
        val wooResult = globalAttributeStore.fetchAttributeTerms(
            selectedSite.get(),
            remoteAttributeId,
            page,
            pageSize
        )
        return wooResult?.model?.map { it.toAppModel() } ?: emptyList()
    }

    /**
     * Saves only the list of attributes for a product
     */
    suspend fun updateProductAttributes(remoteProductId: Long, attributes: List<ProductAttribute>): Boolean {
        val wooResult = productStore.submitProductAttributeChanges(
            selectedSite.get(),
            remoteProductId,
            attributes.map { it.toDataModel() }
        )
        return !wooResult.isError
    }

    /**
     * Returns a list of global attributes from the local db
     */
    fun getGlobalAttributes(): List<ProductGlobalAttribute> {
        val wooResult = globalAttributeStore.loadCachedStoreAttributes(selectedSite.get())
        return wooResult.model?.map { it.toAppModel() } ?: emptyList()
    }

    private fun getCachedWCProductModel(remoteProductId: Long) =
        productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)

    fun getProduct(remoteProductId: Long): Product? = getCachedWCProductModel(remoteProductId)?.toAppModel()

    suspend fun getProductAsync(remoteProductId: Long): Product? = withContext(coroutineDispatchers.io) {
        getCachedWCProductModel(remoteProductId)?.toAppModel()
    }

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

    fun getQuantityRules(remoteProductId: Long): QuantityRules? {
        val product = getCachedWCProductModel(remoteProductId)
        return product?.let {
            QuantityRules(
                if (product.minAllowedQuantity > 0) product.minAllowedQuantity else null,
                if (product.maxAllowedQuantity > 0) product.maxAllowedQuantity else null,
                if (product.groupOfQuantity > 0) product.groupOfQuantity else null
            )
        }
    }

    fun getProductMetadata(remoteProductId: Long): Map<String, Any>? {
        val metadata = getCachedWCProductModel(remoteProductId)?.metadata ?: return null
        val metadataArray = gson.fromJson(metadata, Array<WCMetaData>::class.java)

        return metadataArray?.filter { metadataItem -> metadataItem.key.isNullOrEmpty().not() }
            ?.associate { metadataItem -> metadataItem.key!! to metadataItem.value }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductPasswordChanged(event: OnProductPasswordChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_PASSWORD && event.remoteProductId == remoteProductId) {
            if (event.isError) {
                continuationFetchProductPassword.continueWith(null)
            } else {
                continuationFetchProductPassword.continueWith(event.password)
            }
        } else if (event.causeOfChange == UPDATE_PRODUCT_PASSWORD && event.remoteProductId == remoteProductId) {
            if (event.isError) {
                continuationUpdateProductPassword.continueWith(false)
            } else {
                continuationUpdateProductPassword.continueWith(true)
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductUpdated(event: OnProductUpdated) {
        if (event.causeOfChange == UPDATED_PRODUCT) {
            if (event.isError) {
                AnalyticsTracker.track(
                    PRODUCT_DETAIL_UPDATE_ERROR,
                    mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message
                    )
                )

                val pair = Pair(false, event.error)
                continuationUpdateProduct?.resume(pair)
            } else {
                AnalyticsTracker.track(PRODUCT_DETAIL_UPDATE_SUCCESS)
                val pair = Pair(true, null)
                continuationUpdateProduct?.resume(pair)
            }
            continuationUpdateProduct = null
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductSkuAvailabilityChanged(event: OnProductSkuAvailabilityChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_SKU_AVAILABILITY) {
            // TODO: add event to track sku availability success
            continuationVerifySku.continueWith(event.available)
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
                continuationFetchProductShippingClass.continueWith(false)
            } else {
                continuationFetchProductShippingClass.continueWith(true)
            }
        }
    }

    /**
     * A new product has been added
     */

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
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
