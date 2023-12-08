package com.woocommerce.android.ui.products.categories

import com.woocommerce.android.AppConstants
import com.woocommerce.android.OnChangedException
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.toProductCategory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.ADDED_PRODUCT_CATEGORY
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCategoryChanged
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.TERM_EXISTS
import javax.inject.Inject

class ProductCategoriesRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val PRODUCT_CATEGORIES_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE
    }

    private var addProductCategoryContinuation = ContinuationWrapper<RequestResult>(WooLog.T.PRODUCTS)
    private var offset = 0

    var canLoadMoreProductCategories = true

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Submits a fetch request to get a list of products categories for the current site
     * and returns the full list of product categories from the database
     */
    suspend fun fetchProductCategories(loadMore: Boolean = false): Result<List<ProductCategory>> {
        offset = if (loadMore) offset + PRODUCT_CATEGORIES_PAGE_SIZE else 0
        val payload = WCProductStore.FetchProductCategoriesPayload(
            selectedSite.get(),
            pageSize = PRODUCT_CATEGORIES_PAGE_SIZE,
            offset = offset
        )
        val action = WCProductActionBuilder.newFetchProductCategoriesAction(payload)
        val result: OnProductCategoryChanged = dispatcher.dispatchAndAwait(action)

        return if (result.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.PRODUCT_CATEGORIES_LOAD_FAILED,
                this.javaClass.simpleName,
                result.error.type.toString(),
                result.error.message
            )
            Result.failure(OnChangedException(result.error, result.error.message))
        } else {
            canLoadMoreProductCategories = result.canLoadMore
            AnalyticsTracker.track(AnalyticsEvent.PRODUCT_CATEGORIES_LOADED)
            Result.success(getProductCategoriesList())
        }
    }

    /**
     * Returns all product categories for the current site that are in the database
     */
    fun getProductCategoriesList(): List<ProductCategory> {
        return productStore.getProductCategoriesForSite(selectedSite.get())
            .map { it.toProductCategory() }
    }

    fun getProductCategoryByRemoteId(remoteId: Long) =
        productStore.getProductCategoryByRemoteId(selectedSite.get(), remoteId)

    fun getProductCategoryByNameAndParentId(categoryName: String, parentId: Long): ProductCategory? =
        productStore.getProductCategoryByNameAndParentId(selectedSite.get(), categoryName, parentId)
            ?.toProductCategory()

    /**
     * Fires the request to add a new product category
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun addProductCategory(categoryName: String, parentId: Long): RequestResult {
        val result = addProductCategoryContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val productCategoryModel = WCProductCategoryModel().apply {
                name = categoryName
                parent = parentId
            }
            val payload = WCProductStore.AddProductCategoryPayload(selectedSite.get(), productCategoryModel)
            dispatcher.dispatch(WCProductActionBuilder.newAddProductCategoryAction(payload))
        }

        return when (result) {
            is Cancellation -> RequestResult.NO_ACTION_NEEDED
            is Success -> result.value
        }
    }

    suspend fun updateProductCategory(category: ProductCategory): Result<Unit> {
        val result = productStore.updateProductCategory(
            site = selectedSite.get(),
            id = category.remoteCategoryId,
            payload = ProductRestClient.UpdateProductCategoryPayload(
                name = category.name,
                parentId = category.parentId
            )
        )

        return when {
            result.isError -> {
                WooLog.e(
                    tag = WooLog.T.PRODUCTS,
                    message = "Error updating product category: ${result.error.type}, ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            }

            else -> Result.success(Unit)
        }
    }

    suspend fun deleteProductCategory(category: ProductCategory): Result<Unit> {
        val result = productStore.deleteProductCategory(selectedSite.get(), category.remoteCategoryId)
        return when {
            result.isError -> {
                WooLog.e(
                    tag = WooLog.T.PRODUCTS,
                    message = "Error deleting product category: ${result.error.type}, ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            }

            else -> Result.success(Unit)
        }
    }

    suspend fun addProductCategories(categories: List<ProductCategory>): Result<List<ProductCategory>> {
        val result = productStore.addProductCategories(
            site = selectedSite.get(),
            categories = categories.map {
                WCProductCategoryModel().apply {
                    name = it.name
                    parent = it.parentId
                }
            }
        )

        return when {
            result.isError -> {
                WooLog.e(
                    tag = WooLog.T.PRODUCTS,
                    message = "Error adding product categories: ${result.error.type}, ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            }

            else -> Result.success(result.model!!.map { it.toProductCategory() })
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductCategoriesChanged(event: OnProductCategoryChanged) {
        when (event.causeOfChange) {
            ADDED_PRODUCT_CATEGORY -> {
                if (event.isError) {
                    val requestResultType = if (event.error.type == TERM_EXISTS) {
                        RequestResult.API_ERROR
                    } else RequestResult.ERROR
                    addProductCategoryContinuation.continueWith(requestResultType)
                    AnalyticsTracker.track(
                        AnalyticsEvent.PARENT_CATEGORIES_LOAD_FAILED,
                        this.javaClass.simpleName,
                        event.error.type.toString(),
                        event.error.message
                    )
                } else {
                    AnalyticsTracker.track(AnalyticsEvent.PARENT_CATEGORIES_LOADED)
                    addProductCategoryContinuation.continueWith(RequestResult.SUCCESS)
                }
            }

            else -> {
            }
        }
    }
}
