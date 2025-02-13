package org.wordpress.android.fluxc.store

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.flow.Flow
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.VariationAttributes
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductComponent
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto
import org.wordpress.android.fluxc.model.metadata.MetadataChanges
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue
import org.wordpress.android.fluxc.model.metadata.get
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers.MappingRemoteException
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers.RemoteAddonMapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.BatchProductVariationsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductStockStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductVariationMapper
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.ProductStorageHelper
import org.wordpress.android.fluxc.persistence.dao.AddonsDao
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting.NAME_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.API
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LargeClass")
@Singleton
class WCProductStore @Inject constructor(
    dispatcher: Dispatcher,
    private val wcProductRestClient: ProductRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val addonsDao: AddonsDao,
    private val productStorageHelper: ProductStorageHelper,
    private val logger: AppLogWrapper
) : Store(dispatcher) {
    companion object {
        const val NUM_REVIEWS_PER_FETCH = 25
        const val DEFAULT_PRODUCT_PAGE_SIZE = 25
        const val DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE = 100
        const val DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE = 25
        const val DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE = 25
        const val DEFAULT_PRODUCT_TAGS_PAGE_SIZE = 100
        val DEFAULT_PRODUCT_SORTING = TITLE_ASC
        val DEFAULT_CATEGORY_SORTING = NAME_ASC
        const val VARIATIONS_CREATION_LIMIT = 100
    }

    sealed class IncludeType(val value: String) {
        data object Simple : IncludeType("simple")
        data object Variable : IncludeType("variable")
        data object External : IncludeType("external")
        data object Grouped : IncludeType("grouped")

        companion object {
            fun fromValue(value: String): IncludeType? = when (value) {
                "simple" -> Simple
                "variable" -> Variable
                "external" -> External
                "grouped" -> Grouped
                else -> null
            }
        }
    }

    /**
     * Defines the filter options currently supported in the app
     */
    enum class ProductFilterOption {
        STOCK_STATUS, STATUS, TYPE, CATEGORY, DOWNLOADABLE;

        override fun toString() = name.lowercase(Locale.US)
    }

    enum class DownloadableOptions {
        TRUE, FALSE;

        override fun toString() = name.lowercase(Locale.US)
    }

    enum class VariationFilterOption {
        STATUS, DOWNLOADABLE;

        override fun toString() = name.lowercase(Locale.US)
    }

    enum class SkuSearchOptions {
        Disabled, ExactSearch, PartialMatch
    }

    class FetchProductSkuAvailabilityPayload(
        var site: SiteModel,
        var sku: String
    ) : Payload<BaseNetworkError>()

    class FetchSingleProductPayload(
        var site: SiteModel,
        var remoteProductId: Long
    ) : Payload<BaseNetworkError>()

    class FetchProductsPayload(
        var site: SiteModel,
        var pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        var offset: Int = 0,
        var sorting: ProductSorting = DEFAULT_PRODUCT_SORTING,
        var remoteProductIds: List<Long>? = null,
        var filterOptions: Map<ProductFilterOption, String>? = null,
        var excludedProductIds: List<Long>? = null
    ) : Payload<BaseNetworkError>()

    class SearchProductsPayload(
        var site: SiteModel,
        var searchQuery: String,
        var skuSearchOptions: SkuSearchOptions = SkuSearchOptions.Disabled,
        var pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        var offset: Int = 0,
        var sorting: ProductSorting = DEFAULT_PRODUCT_SORTING,
        var excludedProductIds: List<Long>? = null,
        var filterOptions: Map<ProductFilterOption, String>? = null,
    ) : Payload<BaseNetworkError>()

    class SearchProductsByGlobalUniqueIdPayload(
        var site: SiteModel,
        var globalUniqueId: String,
        var pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        var offset: Int = 0,
        var sorting: ProductSorting = DEFAULT_PRODUCT_SORTING,
        var excludedProductIds: List<Long>? = null,
        var filterOptions: Map<ProductFilterOption, String>? = null,
    ) : Payload<BaseNetworkError>()

    class FetchProductVariationsPayload(
        var site: SiteModel,
        var remoteProductId: Long,
        var pageSize: Int = DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE,
        var offset: Int = 0
    ) : Payload<BaseNetworkError>()

    class FetchProductShippingClassListPayload(
        var site: SiteModel,
        var pageSize: Int = DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE,
        var offset: Int = 0
    ) : Payload<BaseNetworkError>()

    class FetchSingleProductShippingClassPayload(
        var site: SiteModel,
        var remoteShippingClassId: Long
    ) : Payload<BaseNetworkError>()

    class FetchProductReviewsPayload(
        var site: SiteModel,
        var offset: Int = 0,
        var reviewIds: List<Long>? = null,
        var productIds: List<Long>? = null,
        var filterByStatus: List<String>? = null
    ) : Payload<BaseNetworkError>()

    class FetchSingleProductReviewPayload(
        var site: SiteModel,
        var remoteReviewId: Long
    ) : Payload<BaseNetworkError>()

    class FetchProductPasswordPayload(
        var site: SiteModel,
        var remoteProductId: Long
    ) : Payload<BaseNetworkError>()

    class UpdateProductPasswordPayload(
        var site: SiteModel,
        var remoteProductId: Long,
        var password: String
    ) : Payload<BaseNetworkError>()

    class UpdateProductReviewStatusPayload(
        var site: SiteModel,
        var remoteReviewId: Long,
        var newStatus: String
    ) : Payload<BaseNetworkError>()

    class UpdateProductImagesPayload(
        var site: SiteModel,
        var remoteProductId: Long,
        var imageList: List<WCProductImageModel>
    ) : Payload<BaseNetworkError>()

    class UpdateProductPayload(
        var site: SiteModel,
        val product: WCProductModel,
        val metadataChanges: MetadataChanges? = null
    ) : Payload<BaseNetworkError>()

    class BatchUpdateProductsPayload(
        val site: SiteModel,
        val updatedProducts: List<WCProductModel>
    ) : Payload<BaseNetworkError>()

    class UpdateVariationPayload(
        var site: SiteModel,
        val variation: WCProductVariationModel
    ) : Payload<BaseNetworkError>()

    class BatchGenerateVariationsPayload(
        val site: SiteModel,
        val remoteProductId: Long,
        val variations: List<VariationAttributes>
    ) : Payload<BaseNetworkError>()

    /**
     * Payload used by [batchUpdateVariations] function.
     *
     * @param remoteProductId Id of the product.
     * @param remoteVariationsIds Ids of variations that are going to be updated.
     * @param modifiedProperties Map of the properties of variation that are going to be updated.
     * Keys correspond to the names of variation properties. Values are the updated properties values.
     */
    class BatchUpdateVariationsPayload(
        val site: SiteModel,
        val remoteProductId: Long,
        val remoteVariationsIds: Collection<Long>,
        val modifiedProperties: Map<String, Any>
    ) : Payload<BaseNetworkError>() {
        /**
         * Builder class used for instantiating [BatchUpdateVariationsPayload].
         */
        class Builder(
            private val site: SiteModel,
            private val remoteProductId: Long,
            private val variationsIds: Collection<Long>
        ) {
            private val variationsModifications = mutableMapOf<String, Any>()

            fun regularPrice(regularPrice: String) = apply {
                variationsModifications["regular_price"] = regularPrice
            }

            fun salePrice(salePrice: String) = apply {
                variationsModifications["sale_price"] = salePrice
            }

            fun startOfSale(startOfSale: String) = apply {
                variationsModifications["date_on_sale_from"] = startOfSale
            }

            fun endOfSale(endOfSale: String) = apply {
                variationsModifications["date_on_sale_to"] = endOfSale
            }

            fun stockQuantity(stockQuantity: Int) = apply {
                variationsModifications["stock_quantity"] = stockQuantity
            }

            fun stockStatus(stockStatus: CoreProductStockStatus) = apply {
                variationsModifications["stock_status"] = stockStatus
            }

            fun weight(weight: String) = apply {
                variationsModifications["weight"] = weight
            }

            fun dimensions(length: String, width: String, height: String) = apply {
                val dimensions = JsonObject().apply {
                    add("length", JsonPrimitive(length))
                    add("width", JsonPrimitive(width))
                    add("height", JsonPrimitive(height))
                }
                variationsModifications["dimensions"] = dimensions
            }

            fun shippingClassId(shippingClassId: String) = apply {
                variationsModifications["shipping_class_id"] = shippingClassId
            }

            fun shippingClassSlug(shippingClassSlug: String) = apply {
                variationsModifications["shipping_class"] = shippingClassSlug
            }

            fun build() = BatchUpdateVariationsPayload(
                site,
                remoteProductId,
                variationsIds,
                variationsModifications
            )
        }
    }

    class FetchProductCategoriesPayload(
        var site: SiteModel,
        var pageSize: Int = DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE,
        var offset: Int = 0,
        var productCategorySorting: ProductCategorySorting = DEFAULT_CATEGORY_SORTING
    ) : Payload<BaseNetworkError>()

    class AddProductCategoryPayload(
        val site: SiteModel,
        val category: WCProductCategoryModel
    ) : Payload<BaseNetworkError>()

    class FetchProductTagsPayload(
        var site: SiteModel,
        var pageSize: Int = DEFAULT_PRODUCT_TAGS_PAGE_SIZE,
        var offset: Int = 0,
        var searchQuery: String? = null
    ) : Payload<BaseNetworkError>()

    class AddProductTagsPayload(
        val site: SiteModel,
        val tags: List<String>
    ) : Payload<BaseNetworkError>()

    class AddProductPayload(
        var site: SiteModel,
        val product: WCProductModel,
        val metadata: Map<String, WCMetaDataValue>? = null
    ) : Payload<BaseNetworkError>()

    class DeleteProductPayload(
        var site: SiteModel,
        val remoteProductId: Long,
        val forceDelete: Boolean = false
    ) : Payload<BaseNetworkError>()

    enum class ProductErrorType {
        INVALID_PRODUCT_ID,
        INVALID_PARAM,
        INVALID_REVIEW_ID,
        INVALID_IMAGE_ID,
        DUPLICATE_SKU,

        // indicates duplicate term name. Currently only used when adding product categories
        TERM_EXISTS,

        // Happens if a store is running Woo 4.6 and below and tries to delete the product image
        // from a variation. See this PR for more detail:
        // https://github.com/woocommerce/woocommerce/pull/27299
        INVALID_VARIATION_IMAGE_ID,
        INVALID_MIN_MAX_QUANTITY,

        PARSE_ERROR,
        GENERIC_ERROR;

        companion object {
            private val reverseMap = values().associateBy(ProductErrorType::name)
            fun fromString(type: String) = reverseMap[type.uppercase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    class ProductError(val type: ProductErrorType = GENERIC_ERROR, val message: String = "") : OnChangedError

    enum class ProductSorting {
        TITLE_ASC,
        TITLE_DESC,
        DATE_ASC,
        DATE_DESC
    }

    enum class ProductCategorySorting {
        NAME_ASC,
        NAME_DESC
    }

    class RemoteProductSkuAvailabilityPayload(
        val site: SiteModel,
        var sku: String,
        val available: Boolean
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            sku: String,
            available: Boolean
        ) : this(site, sku, available) {
            this.error = error
        }
    }

    class RemoteProductPayload(
        val productWithMetaData: ProductWithMetaData,
        val site: SiteModel
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            product: ProductWithMetaData,
            site: SiteModel
        ) : this(product, site) {
            this.error = error
        }
    }

    class RemoteVariationPayload(
        val variation: WCProductVariationModel,
        val site: SiteModel
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            variation: WCProductVariationModel,
            site: SiteModel
        ) : this(variation, site) {
            this.error = error
        }
    }

    class RemoteProductPasswordPayload(
        val remoteProductId: Long,
        val site: SiteModel,
        val password: String
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            remoteProductId: Long,
            site: SiteModel,
            password: String
        ) : this(remoteProductId, site, password) {
            this.error = error
        }
    }

    class RemoteUpdatedProductPasswordPayload(
        val remoteProductId: Long,
        val site: SiteModel,
        val password: String
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            remoteProductId: Long,
            site: SiteModel,
            password: String
        ) : this(remoteProductId, site, password) {
            this.error = error
        }
    }

    class RemoteProductListPayload(
        val site: SiteModel,
        val productsWithMetaData: List<ProductWithMetaData> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false,
        val remoteProductIds: List<Long>? = null,
        val excludedProductIds: List<Long>? = null
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel
        ) : this(site) {
            this.error = error
        }
    }

    class RemoteSearchProductsPayload(
        var site: SiteModel,
        var searchQuery: String?,
        var skuSearchOptions: SkuSearchOptions,
        var globalUniqueIdSearchQuery: String?,
        var productsWithMetaData: List<ProductWithMetaData> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false,
        var filterOptions: Map<ProductFilterOption, String>? = null
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            query: String?,
            skuSearchOptions: SkuSearchOptions,
            globalUniqueIdSearchQuery: String?,
            filterOptions: Map<ProductFilterOption, String>?
        ) : this(
            site = site,
            searchQuery = query,
            globalUniqueIdSearchQuery = globalUniqueIdSearchQuery,
            skuSearchOptions = skuSearchOptions,
            filterOptions = filterOptions
        ) {
            this.error = error
        }
    }

    class RemoteUpdateProductImagesPayload(
        var site: SiteModel,
        val productWithMetaData: ProductWithMetaData
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            product: ProductWithMetaData
        ) : this(site, product) {
            this.error = error
        }
    }

    class RemoteUpdateProductPayload(
        var site: SiteModel,
        val productWithMetaData: ProductWithMetaData
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            product: ProductWithMetaData
        ) : this(site, product) {
            this.error = error
        }
    }

    class RemoteUpdateVariationPayload(
        var site: SiteModel,
        val variation: WCProductVariationModel
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            variation: WCProductVariationModel
        ) : this(site, variation) {
            this.error = error
        }
    }

    class RemoteProductVariationsPayload(
        val site: SiteModel,
        val remoteProductId: Long,
        val variations: List<WCProductVariationModel> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            remoteProductId: Long
        ) : this(site, remoteProductId) {
            this.error = error
        }
    }

    class RemoteProductShippingClassListPayload(
        val site: SiteModel,
        val shippingClassList: List<WCProductShippingClassModel> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel
        ) : this(site) {
            this.error = error
        }
    }

    class RemoteProductShippingClassPayload(
        val productShippingClassModel: WCProductShippingClassModel,
        val site: SiteModel
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            productShippingClassModel: WCProductShippingClassModel,
            site: SiteModel
        ) : this(productShippingClassModel, site) {
            this.error = error
        }
    }

    class RemoteProductReviewPayload(
        val site: SiteModel,
        val productReview: WCProductReviewModel? = null
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel
        ) : this(site) {
            this.error = error
        }
    }

    class PostReviewReply(
        val site: SiteModel,
        val productId: RemoteId,
        val reviewId: RemoteId,
        val replyContent: String?
    )

    class FetchProductReviewsResponsePayload(
        val site: SiteModel,
        val reviews: List<WCProductReviewModel> = emptyList(),
        val filterProductIds: List<Long>? = null,
        val filterByStatus: List<String>? = null,
        val canLoadMore: Boolean = false
    ) : Payload<ProductError>() {
        constructor(error: ProductError, site: SiteModel) : this(site) {
            this.error = error
        }
    }

    class RemoteProductCategoriesPayload(
        val site: SiteModel,
        val categories: List<WCProductCategoryModel> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel
        ) : this(site) {
            this.error = error
        }
    }

    class RemoteAddProductCategoryResponsePayload(
        val site: SiteModel,
        val category: WCProductCategoryModel?
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            category: WCProductCategoryModel?
        ) : this(site, category) {
            this.error = error
        }
    }

    class RemoteProductTagsPayload(
        val site: SiteModel,
        val tags: List<WCProductTagModel> = emptyList(),
        var offset: Int = 0,
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false,
        var searchQuery: String? = null
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel
        ) : this(site) {
            this.error = error
        }
    }

    class RemoteAddProductTagsResponsePayload(
        val site: SiteModel,
        val tags: List<WCProductTagModel> = emptyList()
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            addedTags: List<WCProductTagModel> = emptyList()
        ) : this(site, addedTags) {
            this.error = error
        }
    }

    class RemoteAddProductPayload(
        var site: SiteModel,
        val productWithMetaData: ProductWithMetaData
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            product: ProductWithMetaData
        ) : this(site, product) {
            this.error = error
        }
    }

    class RemoteDeleteProductPayload(
        var site: SiteModel,
        val remoteProductId: Long
    ) : Payload<ProductError>() {
        constructor(
            error: ProductError,
            site: SiteModel,
            remoteProductId: Long
        ) : this(site, remoteProductId) {
            this.error = error
        }
    }

    // OnChanged events
    class OnProductChanged(
        var rowsAffected: Int,
        var remoteProductId: Long = 0L, // only set for fetching or deleting a single product
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnVariationChanged(
        var remoteProductId: Long = 0L,
        var remoteVariationId: Long = 0L
    ) : OnChanged<ProductError>()

    class OnProductSkuAvailabilityChanged(
        var sku: String,
        var available: Boolean
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductsSearched(
        var searchQuery: String?,
        var isSkuSearch: SkuSearchOptions,
        var globalUniqueIdSearchQuery: String?,
        var searchResults: List<WCProductModel> = emptyList(),
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>()

    class OnProductReviewChanged(
        var rowsAffected: Int,
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductShippingClassesChanged(
        var rowsAffected: Int,
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductImagesChanged(
        var rowsAffected: Int,
        var remoteProductId: Long
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductPasswordChanged(
        var remoteProductId: Long,
        var password: String?
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductUpdated(
        var rowsAffected: Int,
        var remoteProductId: Long
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnVariationUpdated(
        var rowsAffected: Int,
        var remoteProductId: Long,
        var remoteVariationId: Long
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductCategoryChanged(
        var rowsAffected: Int,
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductTagChanged(
        var rowsAffected: Int,
        var canLoadMore: Boolean = false
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    class OnProductCreated(
        var rowsAffected: Int,
        var remoteProductId: Long = 0L
    ) : OnChanged<ProductError>() {
        var causeOfChange: WCProductAction? = null
    }

    /**
     * returns the corresponding product from the database as a [WCProductModel].
     */
    fun getProductByRemoteId(site: SiteModel, remoteProductId: Long): WCProductModel? =
        ProductSqlUtils.getProductByRemoteId(site, remoteProductId)

    /**
     * returns the corresponding variation from the database as a [WCProductVariationModel].
     */
    fun getVariationByRemoteId(
        site: SiteModel,
        remoteProductId: Long,
        remoteVariationId: Long
    ): WCProductVariationModel? =
        ProductSqlUtils.getVariationByRemoteId(site, remoteProductId, remoteVariationId)

    /**
     * returns true if the corresponding product exists in the database
     */
    fun geProductExistsByRemoteId(site: SiteModel, remoteProductId: Long) =
        ProductSqlUtils.geProductExistsByRemoteId(site, remoteProductId)

    /**
     * returns true if the product exists with this [sku] in the database
     */
    fun geProductExistsBySku(site: SiteModel, sku: String) =
        ProductSqlUtils.getProductExistsBySku(site, sku)

    /**
     * returns a list of variations for a specific product in the database
     */
    fun getVariationsForProduct(site: SiteModel, remoteProductId: Long): List<WCProductVariationModel> =
        ProductSqlUtils.getVariationsForProduct(site, remoteProductId)

    /**
     * returns a list of shipping classes for a specific site in the database
     */
    fun getShippingClassListForSite(site: SiteModel): List<WCProductShippingClassModel> =
        ProductSqlUtils.getProductShippingClassListForSite(site.id)

    /**
     * returns the corresponding product shipping class from the database as a [WCProductShippingClassModel].
     */
    fun getShippingClassByRemoteId(site: SiteModel, remoteShippingClassId: Long): WCProductShippingClassModel? =
        ProductSqlUtils.getProductShippingClassByRemoteId(remoteShippingClassId, site.id)

    /**
     * returns a list of [WCProductModel] for the give [SiteModel] and [remoteProductIds]
     * if it exists in the database
     */
    fun getProductsByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): List<WCProductModel> =
        ProductSqlUtils.getProductsByRemoteIds(site, remoteProductIds)

    /**
     * Returns a list of [WCProductModel] for the given [SiteModel], [filterOptions] and [searchQuery].
     * To filter by category, make sure the [filterOptions] value is the category ID in String.
     */
    fun getProducts(
        site: SiteModel,
        filterOptions: Map<ProductFilterOption, String>,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        excludedProductIds: List<Long>? = null,
        searchQuery: String? = null,
        skuSearchOptions: SkuSearchOptions = SkuSearchOptions.Disabled,
    ): List<WCProductModel> =
        ProductSqlUtils.getProducts(site, filterOptions, sortType, excludedProductIds, searchQuery, skuSearchOptions)

    fun getProductsForSite(site: SiteModel, sortType: ProductSorting = DEFAULT_PRODUCT_SORTING) =
        ProductSqlUtils.getProductsForSite(site, sortType)

    fun deleteProductsForSite(site: SiteModel) = ProductSqlUtils.deleteProductsForSite(site)

    fun getProductReviewsForSite(site: SiteModel): List<WCProductReviewModel> =
        ProductSqlUtils.getProductReviewsForSite(site)

    fun getProductReviewsByReviewId(reviewIds: List<Long>): List<WCProductReviewModel> =
        ProductSqlUtils.getProductReviewsByReviewIds(reviewIds)

    fun getProductReviewsForProductAndSiteId(localSiteId: Int, remoteProductId: Long): List<WCProductReviewModel> =
        ProductSqlUtils.getProductReviewsForProductAndSiteId(localSiteId, remoteProductId)

    /**
     * returns the count of products for the given [SiteModel] and [remoteProductIds]
     * if it exists in the database
     */
    fun getProductCountByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): Int =
        ProductSqlUtils.getProductCountByRemoteIds(site, remoteProductIds)

    /**
     * returns the count of virtual products for the given [SiteModel] and [remoteProductIds]
     * if it exists in the database
     */
    fun getVirtualProductCountByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): Int =
        ProductSqlUtils.getVirtualProductCountByRemoteIds(site, remoteProductIds)

    /**
     * returns a list of tags for a specific site in the database
     */
    fun getTagsForSite(site: SiteModel): List<WCProductTagModel> =
        ProductSqlUtils.getProductTagsForSite(site.id)

    fun getProductTagsByNames(site: SiteModel, tagNames: List<String>) =
        ProductSqlUtils.getProductTagsByNames(site.id, tagNames)

    fun getProductTagByName(site: SiteModel, tagName: String) =
        ProductSqlUtils.getProductTagByName(site.id, tagName)

    fun getProductReviewByRemoteId(
        localSiteId: Int,
        remoteReviewId: Long
    ): WCProductReviewModel? = ProductSqlUtils
        .getProductReviewByRemoteId(localSiteId, remoteReviewId)

    fun deleteProductReviewsForSite(site: SiteModel) = ProductSqlUtils.deleteAllProductReviewsForSite(site)

    fun deleteAllProductReviews() = ProductSqlUtils.deleteAllProductReviews()

    fun deleteProductImage(site: SiteModel, remoteProductId: Long, remoteMediaId: Long) =
        ProductSqlUtils.deleteProductImage(site, remoteProductId, remoteMediaId)

    fun getProductCategoriesForSite(site: SiteModel, sortType: ProductCategorySorting = DEFAULT_CATEGORY_SORTING) =
        ProductSqlUtils.getProductCategoriesForSite(site, sortType)

    fun getProductCategoryByRemoteId(site: SiteModel, remoteId: Long) =
        ProductSqlUtils.getProductCategoryByRemoteId(site.id, remoteId)

    fun getProductCategoryByNameAndParentId(
        site: SiteModel,
        categoryName: String,
        parentId: Long = 0L
    ) = ProductSqlUtils.getProductCategoryByNameAndParentId(site.id, categoryName, parentId)

    @Suppress("LongMethod", "ComplexMethod")
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCProductAction ?: return
        when (actionType) {
            // remote actions
            WCProductAction.FETCH_PRODUCT_SKU_AVAILABILITY ->
                fetchProductSkuAvailability(action.payload as FetchProductSkuAvailabilityPayload)

            WCProductAction.FETCH_PRODUCTS ->
                fetchProducts(action.payload as FetchProductsPayload)

            WCProductAction.SEARCH_PRODUCTS ->
                searchProducts(action.payload as SearchProductsPayload)

            WCProductAction.SEARCH_PRODUCTS_BY_GLOBAL_UNIQUE_ID ->
                searchProductsByGlobalUniqueId(action.payload as SearchProductsByGlobalUniqueIdPayload)

            WCProductAction.UPDATE_PRODUCT_IMAGES ->
                updateProductImages(action.payload as UpdateProductImagesPayload)

            WCProductAction.UPDATE_PRODUCT ->
                updateProduct(action.payload as UpdateProductPayload)

            WCProductAction.FETCH_SINGLE_PRODUCT_SHIPPING_CLASS ->
                fetchProductShippingClass(action.payload as FetchSingleProductShippingClassPayload)

            WCProductAction.FETCH_PRODUCT_SHIPPING_CLASS_LIST ->
                fetchProductShippingClasses(action.payload as FetchProductShippingClassListPayload)

            WCProductAction.FETCH_PRODUCT_PASSWORD ->
                fetchProductPassword(action.payload as FetchProductPasswordPayload)

            WCProductAction.UPDATE_PRODUCT_PASSWORD ->
                updateProductPassword(action.payload as UpdateProductPasswordPayload)

            WCProductAction.FETCH_PRODUCT_CATEGORIES ->
                fetchProductCategories(action.payload as FetchProductCategoriesPayload)

            WCProductAction.FETCH_PRODUCT_TAGS ->
                fetchProductTags(action.payload as FetchProductTagsPayload)

            WCProductAction.ADD_PRODUCT_TAGS ->
                addProductTags(action.payload as AddProductTagsPayload)

            WCProductAction.ADD_PRODUCT ->
                addProduct(action.payload as AddProductPayload)

            WCProductAction.DELETE_PRODUCT ->
                deleteProduct(action.payload as DeleteProductPayload)

            // remote responses
            WCProductAction.FETCHED_PRODUCT_SKU_AVAILABILITY ->
                handleFetchProductSkuAvailabilityCompleted(action.payload as RemoteProductSkuAvailabilityPayload)

            WCProductAction.FETCHED_PRODUCTS ->
                handleFetchProductsCompleted(action.payload as RemoteProductListPayload)

            WCProductAction.SEARCHED_PRODUCTS ->
                handleSearchProductsCompleted(action.payload as RemoteSearchProductsPayload)

            WCProductAction.UPDATED_PRODUCT_IMAGES ->
                handleUpdateProductImages(action.payload as RemoteUpdateProductImagesPayload)

            WCProductAction.UPDATED_PRODUCT ->
                handleUpdateProduct(action.payload as RemoteUpdateProductPayload)

            WCProductAction.FETCHED_PRODUCT_SHIPPING_CLASS_LIST ->
                handleFetchProductShippingClassesCompleted(action.payload as RemoteProductShippingClassListPayload)

            WCProductAction.FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS ->
                handleFetchProductShippingClassCompleted(action.payload as RemoteProductShippingClassPayload)

            WCProductAction.FETCHED_PRODUCT_PASSWORD ->
                handleFetchProductPasswordCompleted(action.payload as RemoteProductPasswordPayload)

            WCProductAction.UPDATED_PRODUCT_PASSWORD ->
                handleUpdatedProductPasswordCompleted(action.payload as RemoteUpdatedProductPasswordPayload)

            WCProductAction.FETCHED_PRODUCT_CATEGORIES ->
                handleFetchProductCategories(action.payload as RemoteProductCategoriesPayload)

            WCProductAction.ADDED_PRODUCT_CATEGORY ->
                handleAddProductCategory(action.payload as RemoteAddProductCategoryResponsePayload)

            WCProductAction.FETCHED_PRODUCT_TAGS ->
                handleFetchProductTagsCompleted(action.payload as RemoteProductTagsPayload)

            WCProductAction.ADDED_PRODUCT_TAGS ->
                handleAddProductTags(action.payload as RemoteAddProductTagsResponsePayload)

            WCProductAction.ADDED_PRODUCT ->
                handleAddNewProduct(action.payload as RemoteAddProductPayload)

            WCProductAction.DELETED_PRODUCT ->
                handleDeleteProduct(action.payload as RemoteDeleteProductPayload)
        }
    }

    fun observeProducts(
        site: SiteModel,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        filterOptions: Map<ProductFilterOption, String> = emptyMap(),
        excludeSampleProducts: Boolean = false,
        limit: Int? = null
    ): Flow<List<WCProductModel>> = ProductSqlUtils.observeProducts(
        site = site,
        sortType = sortType,
        filterOptions = filterOptions,
        excludeSampleProducts = excludeSampleProducts,
        limit = limit
    )

    fun observeProductsCount(
        site: SiteModel,
        filterOptions: Map<ProductFilterOption, String> = emptyMap(),
        excludeSampleProducts: Boolean = false
    ): Flow<Long> = ProductSqlUtils.observeProductsCount(
        site = site,
        filterOptions = filterOptions,
        excludeSampleProducts = excludeSampleProducts
    )

    fun observeVariations(site: SiteModel, productId: Long): Flow<List<WCProductVariationModel>> =
        ProductSqlUtils.observeVariations(site, productId)

    fun observeCategories(
        site: SiteModel,
        sortType: ProductCategorySorting = DEFAULT_CATEGORY_SORTING
    ): Flow<List<WCProductCategoryModel>> =
        ProductSqlUtils.observeCategories(site, sortType)

    fun observeBundledProducts(
        site: SiteModel,
        remoteProductId: Long
    ) = ProductSqlUtils.observeBundledProducts(site, remoteProductId)

    suspend fun getBundledProductsCount(site: SiteModel, remoteProductId: Long): Int {
        return ProductSqlUtils.getBundledProductsCount(site, remoteProductId)
    }

    suspend fun getCompositeProducts(site: SiteModel, remoteProductId: Long): List<WCProductComponent> {
        return ProductSqlUtils.getCompositeProducts(site, remoteProductId)
    }

    suspend fun submitProductAttributeChanges(
        site: SiteModel,
        productId: Long,
        attributes: List<WCProductModel.ProductAttribute>
    ): WooResult<WCProductModel> =
        coroutineEngine.withDefaultContext(API, this, "submitProductAttributes") {
            wcProductRestClient.updateProductAttributes(site, productId, Gson().toJson(attributes))
                .also { payload ->
                    payload.result?.let { productStorageHelper.upsertProduct(it) }
                }
                .asWooResult { it.product }
        }

    suspend fun submitVariationAttributeChanges(
        site: SiteModel,
        productId: Long,
        variationId: Long,
        attributes: List<WCProductModel.ProductAttribute>
    ): WooResult<WCProductVariationModel> =
        coroutineEngine.withDefaultContext(API, this, "submitVariationAttributes") {
            wcProductRestClient.updateVariationAttributes(site, productId, variationId, Gson().toJson(attributes))
                .asWooResult()
                .model?.asProductVariationModel()
                ?.apply {
                    ProductSqlUtils.insertOrUpdateProductVariation(this)
                }
                ?.let { WooResult(it) }
        } ?: WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))

    suspend fun generateEmptyVariation(
        site: SiteModel,
        product: WCProductModel
    ): WooResult<WCProductVariationModel> =
        coroutineEngine.withDefaultContext(API, this, "generateEmptyVariation") {
            product.attributeList
                .filter { it.variation }
                .map { ProductVariantOption(it.id, it.name, "") }
                .let { Gson().toJson(it) }
                .let { wcProductRestClient.generateEmptyVariation(site, product.remoteProductId, it) }
                .asWooResult()
                .model?.asProductVariationModel()
                ?.apply {
                    ProductSqlUtils.insertOrUpdateProductVariation(this)
                }
                ?.let { WooResult(it) }
                ?: WooResult(WooError(INVALID_RESPONSE, GenericErrorType.INVALID_RESPONSE))
        }

    suspend fun deleteVariation(
        site: SiteModel,
        productId: Long,
        variationId: Long
    ): WooResult<WCProductVariationModel> =
        coroutineEngine.withDefaultContext(API, this, "deleteVariation") {
            wcProductRestClient.deleteVariation(site, productId, variationId)
                .asWooResult()
                .model?.asProductVariationModel()
                ?.apply {
                    ProductSqlUtils.deleteVariationsForProduct(site, productId)
                }
                ?.let { WooResult(it) }
                ?: WooResult(WooError(INVALID_RESPONSE, GenericErrorType.INVALID_RESPONSE))
        }

    override fun onRegister() = AppLog.d(API, "WCProductStore onRegister")

    @Suppress("ForbiddenComment")
    suspend fun fetchSingleProduct(payload: FetchSingleProductPayload): OnProductChanged {
        return coroutineEngine.withDefaultContext(API, this, "fetchSingleProduct") {
            val result = with(payload) { wcProductRestClient.fetchSingleProduct(site, remoteProductId) }

            return@withDefaultContext if (result.isError) {
                OnProductChanged(0).also {
                    it.error = result.error
                    it.remoteProductId = result.productWithMetaData.product.remoteProductId
                }
            } else {
                val rowsAffected = productStorageHelper.upsertProduct(result.productWithMetaData)

                // TODO: 18/08/2021 @wzieba add tests
                coroutineEngine.launch(T.DB, this, "cacheProductAddons") {
                    val domainAddons = extractAddonsFromProductMetaData(result.productWithMetaData.metaData)

                    addonsDao.cacheProductAddons(
                        productRemoteId = result.productWithMetaData.product.remoteId,
                        localSiteId = result.site.localId(),
                        addons = domainAddons
                    )
                }

                OnProductChanged(rowsAffected).also {
                    it.remoteProductId = result.productWithMetaData.product.remoteProductId
                }
            }
        }
    }

    suspend fun fetchSingleVariation(
        site: SiteModel,
        remoteProductId: Long,
        remoteVariationId: Long
    ): OnVariationChanged {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchSingleVariation") {
            val result = wcProductRestClient
                .fetchSingleVariation(site, remoteProductId, remoteVariationId)

            return@withDefaultContext if (result.isError) {
                OnVariationChanged().also {
                    it.error = result.error
                    it.remoteProductId = result.variation.remoteProductId
                    it.remoteVariationId = result.variation.remoteVariationId
                }
            } else {
                ProductSqlUtils.insertOrUpdateProductVariation(result.variation)
                OnVariationChanged().also {
                    it.remoteProductId = result.variation.remoteProductId
                    it.remoteVariationId = result.variation.remoteVariationId
                }
            }
        }
    }

    private fun fetchProductSkuAvailability(payload: FetchProductSkuAvailabilityPayload) {
        with(payload) { wcProductRestClient.fetchProductSkuAvailability(site, sku) }
    }

    private fun fetchProducts(payload: FetchProductsPayload) {
        with(payload) {
            wcProductRestClient.fetchProducts(
                site = site,
                pageSize = pageSize,
                offset = offset,
                sortType = sorting,
                includedProductIds = remoteProductIds,
                filterOptions = filterOptions,
                excludedProductIds = excludedProductIds,
                skuSearchOptions = SkuSearchOptions.Disabled
            )
        }
    }

    suspend fun fetchProductListSynced(site: SiteModel, productIds: List<Long>): List<WCProductModel>? {
        return coroutineEngine.withDefaultContext(API, this, "fetchProductList") {
            wcProductRestClient.fetchProductsWithSyncRequest(site = site, includedProductIds = productIds)
                .result
        }?.also {
            productStorageHelper.upsertProducts(it)
        }?.map { it.product }
    }

    suspend fun fetchProductCategoryListSynced(
        site: SiteModel,
        categoryIds: List<Long>
    ): List<WCProductCategoryModel>? {
        return coroutineEngine.withDefaultContext(API, this, "fetchProductCategoryList") {
            wcProductRestClient.fetchProductsCategoriesWithSyncRequest(
                site = site,
                includedCategoryIds = categoryIds
            ).result
        }?.also {
            ProductSqlUtils.insertOrUpdateProductCategories(it)
        }
    }

    private fun searchProducts(payload: SearchProductsPayload) {
        with(payload) {
            wcProductRestClient.searchProducts(
                site = site,
                searchQuery = searchQuery,
                skuSearchOptions = skuSearchOptions,
                pageSize = pageSize,
                offset = offset,
                sorting = sorting,
                excludedProductIds = excludedProductIds,
                filterOptions = filterOptions
            )
        }
    }

    private fun searchProductsByGlobalUniqueId(payload: SearchProductsByGlobalUniqueIdPayload) {
        with(payload) {
            wcProductRestClient.searchProductsByGlobalUniqueId(
                site = site,
                globalUniqueIdSearchQuery = globalUniqueId,
                pageSize = pageSize,
                offset = offset,
                sorting = sorting,
                excludedProductIds = excludedProductIds,
                filterOptions = filterOptions
            )
        }
    }

    suspend fun fetchProductVariations(payload: FetchProductVariationsPayload): OnProductChanged {
        return coroutineEngine.withDefaultContext(API, this, "fetchProductVariations") {
            val result = with(payload) {
                wcProductRestClient.fetchProductVariations(site, remoteProductId, pageSize, offset)
            }
            return@withDefaultContext if (result.isError) {
                OnProductChanged(0, payload.remoteProductId).also { it.error = result.error }
            } else {
                // delete product variations for site if this is the first page of results, otherwise
                // product variations deleted outside of the app will persist
                if (result.offset == 0) {
                    ProductSqlUtils.deleteVariationsForProduct(result.site, result.remoteProductId)
                }

                val rowsAffected = ProductSqlUtils.insertOrUpdateProductVariations(
                    result.variations
                )
                OnProductChanged(rowsAffected, payload.remoteProductId, canLoadMore = result.canLoadMore)
            }
        }
    }

    private fun fetchProductShippingClass(payload: FetchSingleProductShippingClassPayload) {
        with(payload) { wcProductRestClient.fetchSingleProductShippingClass(site, remoteShippingClassId) }
    }

    private fun fetchProductShippingClasses(payload: FetchProductShippingClassListPayload) {
        with(payload) { wcProductRestClient.fetchProductShippingClassList(site, pageSize, offset) }
    }

    suspend fun fetchProductReviews(
        payload: FetchProductReviewsPayload,
        deletePreviouslyCachedReviews: Boolean
    ): OnProductReviewChanged {
        return coroutineEngine.withDefaultContext(API, this, "fetchProductReviews") {
            val response = with(payload) {
                wcProductRestClient.fetchProductReviews(site, offset, reviewIds, productIds, filterByStatus)
            }

            val onProductReviewChanged = if (response.isError) {
                OnProductReviewChanged(0).also { it.error = response.error }
            } else {
                // Clear existing product reviews if this is a fresh fetch (loadMore = false).
                // This is the simplest way to keep our local reviews in sync with remote reviews
                // in case of deletions or status updates.
                if (deletePreviouslyCachedReviews) {
                    ProductSqlUtils.deleteAllProductReviewsForSite(response.site)
                }
                val rowsAffected = ProductSqlUtils.insertOrUpdateProductReviews(response.reviews)
                OnProductReviewChanged(rowsAffected, canLoadMore = response.canLoadMore)
            }

            onProductReviewChanged
        }
    }

    suspend fun fetchSingleProductReview(payload: FetchSingleProductReviewPayload): OnProductReviewChanged {
        return coroutineEngine.withDefaultContext(API, this, "fetchSingleProductReview") {
            val result = wcProductRestClient.fetchProductReviewById(payload.site, payload.remoteReviewId)

            return@withDefaultContext if (result.isError) {
                OnProductReviewChanged(0).also { it.error = result.error }
            } else {
                val rowsAffected = result.productReview?.let {
                    ProductSqlUtils.insertOrUpdateProductReview(it)
                } ?: 0
                OnProductReviewChanged(rowsAffected)
            }
        }
    }

    private fun fetchProductPassword(payload: FetchProductPasswordPayload) {
        with(payload) { wcProductRestClient.fetchProductPassword(site, remoteProductId) }
    }

    private fun updateProductPassword(payload: UpdateProductPasswordPayload) {
        with(payload) { wcProductRestClient.updateProductPassword(site, remoteProductId, password) }
    }

    suspend fun updateProductReviewStatus(site: SiteModel, reviewId: Long, newStatus: String) =
        coroutineEngine.withDefaultContext(API, this, "updateProductReviewStatus") {
            val result = wcProductRestClient.updateProductReviewStatus(site, reviewId, newStatus)

            return@withDefaultContext if (result.isError) {
                WooResult(result.error)
            } else {
                result.result?.let { review ->
                    if (review.status == "spam" || review.status == "trash") {
                        // Delete this review from the database
                        ProductSqlUtils.deleteProductReview(review)
                    } else {
                        // Insert or update in the database
                        ProductSqlUtils.insertOrUpdateProductReview(review)
                    }
                }
                WooResult(result.result)
            }
        }

    private fun updateProductImages(payload: UpdateProductImagesPayload) {
        with(payload) { wcProductRestClient.updateProductImages(site, remoteProductId, imageList) }
    }

    private fun fetchProductCategories(payloadProduct: FetchProductCategoriesPayload) {
        with(payloadProduct) {
            wcProductRestClient.fetchProductCategories(
                site, pageSize, offset, productCategorySorting
            )
        }
    }

    suspend fun addProductCategories(
        site: SiteModel,
        categories: List<WCProductCategoryModel>
    ): WooResult<List<WCProductCategoryModel>> = coroutineEngine.withDefaultContext(API, this, "addProductCategories") {
        val result = wcProductRestClient.addProductCategories(
            site = site,
            categories = categories
        )

        if (!result.isError) {
            val addedCategories = result.result!!
            if (addedCategories.size < categories.size) {
                AppLog.w(
                    API,
                    "addProductCategories: not all categories were added. " +
                            "Expected: ${categories.size}, added: ${addedCategories.size}"
                )
            }

            addedCategories.forEach { category ->
                ProductSqlUtils.insertOrUpdateProductCategory(category)
            }
        }

        return@withDefaultContext result.asWooResult()
    }

    suspend fun addProductCategory(
        site: SiteModel,
        category: WCProductCategoryModel
    ): WooResult<WCProductCategoryModel> = coroutineEngine.withDefaultContext(API, this, "addProductCategory") {
        val result = wcProductRestClient.addProductCategory(
            site = site,
            category = category
        )
        if (!result.isError) {
            val updatedCategory = result.result!!
            ProductSqlUtils.insertOrUpdateProductCategory(updatedCategory)
        }
        return@withDefaultContext result.asWooResult()
    }

    suspend fun updateProductCategory(
        site: SiteModel,
        category: WCProductCategoryModel
    ): WooResult<WCProductCategoryModel> = coroutineEngine.withDefaultContext(API, this, "updateProductCategory") {
        val result = wcProductRestClient.updateProductCategory(
            site = site,
            category = category
        )
        if (!result.isError) {
            val updatedCategory = result.result!!
            ProductSqlUtils.insertOrUpdateProductCategory(updatedCategory)
        }
        return@withDefaultContext result.asWooResult()
    }

    suspend fun deleteProductCategory(site: SiteModel, remoteId: Long): WooResult<WCProductCategoryModel> =
        coroutineEngine.withDefaultContext(API, this, "deleteProductCategory") {
            val result = wcProductRestClient.deleteProductCategory(
                site = site,
                remoteId = remoteId
            )
            if (!result.isError) {
                val deletedCategory = result.result!!
                ProductSqlUtils.deleteProductCategory(deletedCategory)
            }
            return@withDefaultContext result.asWooResult()
        }

    private fun fetchProductTags(payload: FetchProductTagsPayload) {
        with(payload) { wcProductRestClient.fetchProductTags(site, pageSize, offset, searchQuery) }
    }

    private fun addProductTags(payload: AddProductTagsPayload) {
        with(payload) { wcProductRestClient.addProductTags(site, tags) }
    }

    private fun updateProduct(payload: UpdateProductPayload) {
        with(payload) {
            val storedProduct = getProductByRemoteId(site, product.remoteProductId)
            wcProductRestClient.updateProduct(site, storedProduct, product, payload.metadataChanges)
        }
    }

    suspend fun updateVariation(payload: UpdateVariationPayload): OnVariationUpdated {
        return coroutineEngine.withDefaultContext(API, this, "updateVariation") {
            with(payload) {
                val storedVariation = getVariationByRemoteId(
                    site,
                    variation.remoteProductId,
                    variation.remoteVariationId
                )
                val result: RemoteUpdateVariationPayload = wcProductRestClient.updateVariation(
                    site,
                    storedVariation,
                    variation
                )
                return@withDefaultContext if (result.isError) {
                    OnVariationUpdated(
                        0,
                        result.variation.remoteProductId,
                        result.variation.remoteVariationId
                    ).also { it.error = result.error }
                } else {
                    val rowsAffected = ProductSqlUtils.insertOrUpdateProductVariation(
                        result.variation
                    )
                    OnVariationUpdated(
                        rowsAffected,
                        result.variation.remoteProductId,
                        result.variation.remoteVariationId
                    )
                }
            }
        }
    }

    suspend fun batchUpdateProducts(payload: BatchUpdateProductsPayload): WooResult<List<WCProductModel>> =
        coroutineEngine.withDefaultContext(API, this, "batchUpdateProducts") {
            val existingProducts = ProductSqlUtils.getProductsByRemoteIds(
                site = payload.site,
                remoteProductIds = payload.updatedProducts.map(WCProductModel::remoteProductId)
            )

            val sortedExistingToUpdatedProducts = existingProducts
                .sortedBy(WCProductModel::remoteProductId)
                .zip(payload.updatedProducts.sortedBy(WCProductModel::remoteProductId))
                .toMap()

            with(payload) {
                val result = wcProductRestClient.batchUpdateProducts(
                    site,
                    sortedExistingToUpdatedProducts
                )
                return@withDefaultContext if (result.isError) {
                    WooResult(result.error)
                } else {
                    result.result?.let {
                        productStorageHelper.upsertProducts(it)
                    }
                    WooResult(result.result?.map { it.product })
                }
            }
        }

    /**
     * Batch create variations on the backend and save result locally.
     * For each variant, it only receives the list of attributes. The rest of the variant properties
     * will use the default values.
     *
     * @param payload Instance of [BatchGenerateVariationsPayload].
     */
    suspend fun batchGenerateVariations(payload: BatchGenerateVariationsPayload):
            WooResult<BatchProductVariationsApiResponse> =
        coroutineEngine.withDefaultContext(API, this, "batchCreateVariations") {
            val createVariations = payload.variations.map {
                buildMap { put("attributes", it) }
            }

            with(payload) {
                val result: WooPayload<BatchProductVariationsApiResponse> =
                    wcProductRestClient.batchUpdateVariations(
                        site = site,
                        productId = remoteProductId,
                        createVariations = createVariations
                    )

                return@withDefaultContext if (result.isError) {
                    WooResult(result.error)
                } else {
                    val generatedVariations = result.result?.createdVariations?.map { response ->
                        response.asProductVariationModel().apply {
                            remoteProductId = payload.remoteProductId
                            localSiteId = payload.site.id
                        }
                    } ?: emptyList()
                    ProductSqlUtils.insertOrUpdateProductVariations(generatedVariations)
                    WooResult(result.result)
                }
            }
        }

    /**
     * Batch updates variations on the backend and updates variations locally after successful request.
     *
     * @param payload Instance of [BatchUpdateVariationsPayload]. It can be produced using
     * [BatchUpdateVariationsPayload.Builder] class.
     */
    suspend fun batchUpdateVariations(payload: BatchUpdateVariationsPayload):
            WooResult<BatchProductVariationsApiResponse> =
        coroutineEngine.withDefaultContext(API, this, "batchUpdateVariations") {
            with(payload) {
                val updateVariations: List<Map<String, Any>> = remoteVariationsIds.map { variationId ->
                    modifiedProperties.toMutableMap()
                        .also { properties -> properties["id"] = variationId }
                }
                val result: WooPayload<BatchProductVariationsApiResponse> =
                    wcProductRestClient.batchUpdateVariations(
                        site = site,
                        productId = remoteProductId,
                        updateVariations = updateVariations
                    )

                return@withDefaultContext if (result.isError) {
                    WooResult(result.error)
                } else {
                    val updatedVariations = result.result?.updatedVariations?.map { response ->
                        response.asProductVariationModel().apply {
                            remoteProductId = payload.remoteProductId
                            localSiteId = payload.site.id
                        }
                    } ?: emptyList()
                    ProductSqlUtils.insertOrUpdateProductVariations(updatedVariations)
                    WooResult(result.result)
                }
            }
        }

    suspend fun fetchProductCategories(
        site: SiteModel,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE,
        sortType: ProductCategorySorting = DEFAULT_CATEGORY_SORTING,
        includedCategoryIds: List<Long> = emptyList(),
        excludedCategoryIds: List<Long> = emptyList()
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(API, this, "fetchProductCategories") {
            val response = wcProductRestClient.fetchProductsCategoriesWithSyncRequest(
                site = site,
                offset = offset,
                pageSize = pageSize,
                productCategorySorting = sortType,
                includedCategoryIds = includedCategoryIds,
                excludedCategoryIds = excludedCategoryIds
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    if (offset == 0 && includedCategoryIds.isEmpty() && excludedCategoryIds.isEmpty()) {
                        ProductSqlUtils.deleteAllProductCategories()
                    }
                    ProductSqlUtils.insertOrUpdateProductCategories(response.result)
                    val canLoadMore = response.result.size == pageSize
                    WooResult(canLoadMore)
                }

                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    /**
     * @return Boolean indicating whether more products can be fetched.
     */
    @Suppress("ComplexCondition")
    suspend fun fetchProducts(
        site: SiteModel,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        includedProductIds: List<Long> = emptyList(),
        excludedProductIds: List<Long> = emptyList(),
        filterOptions: Map<ProductFilterOption, String> = emptyMap(),
        includeTypes: List<IncludeType> = emptyList(),
        forceRefresh: Boolean = true,
        orderCurrency: String? = null,
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(API, this, "fetchProducts") {
            val response = wcProductRestClient.fetchProductsWithSyncRequest(
                site = site,
                offset = offset,
                pageSize = pageSize,
                sortType = sortType,
                includedProductIds = includedProductIds,
                excludedProductIds = excludedProductIds,
                filterOptions = filterOptions,
                includeTypes = includeTypes,
                orderCurrency = orderCurrency,
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    if (forceRefresh &&
                        offset == 0 &&
                        includedProductIds.isEmpty() &&
                        excludedProductIds.isEmpty()
                    ) {
                        productStorageHelper.deleteProductsForSite(site)
                    }

                    productStorageHelper.upsertProducts(response.result)
                    val canLoadMore = response.result.size == pageSize
                    WooResult(canLoadMore)
                }

                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun searchProducts(
        site: SiteModel,
        searchString: String,
        skuSearchOptions: SkuSearchOptions = SkuSearchOptions.Disabled,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        orderCurrency: String? = null,
    ): WooResult<ProductSearchResult> {
        return coroutineEngine.withDefaultContext(API, this, "searchProducts") {
            val response = wcProductRestClient.fetchProductsWithSyncRequest(
                site = site,
                offset = offset,
                pageSize = pageSize,
                searchQuery = searchString,
                skuSearchOptions = skuSearchOptions,
                orderCurrency = orderCurrency
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    productStorageHelper.upsertProducts(response.result)
                    val productIds = response.result.map { it.product.remoteProductId }
                    val products = if (productIds.isNotEmpty()) {
                        ProductSqlUtils.getProductsByRemoteIds(site, productIds)
                    } else {
                        emptyList()
                    }
                    val canLoadMore = response.result.size == pageSize
                    WooResult(ProductSearchResult(products, canLoadMore))
                }

                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun searchProductCategories(
        site: SiteModel,
        searchString: String,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE
    ): WooResult<ProductCategorySearchResult> {
        return coroutineEngine.withDefaultContext(
            API,
            this,
            "searchProductCategories"
        ) {
            val response = wcProductRestClient.fetchProductsCategoriesWithSyncRequest(
                site = site,
                offset = offset,
                pageSize = pageSize,
                searchQuery = searchString
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    ProductSqlUtils.insertOrUpdateProductCategories(response.result)
                    val categoryIds = response.result.map { it.remoteCategoryId }
                    val categories = if (categoryIds.isNotEmpty()) {
                        ProductSqlUtils.getProductCategoriesByRemoteIds(site, categoryIds)
                    } else {
                        emptyList()
                    }
                    val canLoadMore = response.result.size == pageSize
                    WooResult(ProductCategorySearchResult(categories, canLoadMore))
                }

                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    /**
     * @return Boolean indicating whether more variations can be fetched.
     */
    suspend fun fetchProductVariations(
        site: SiteModel,
        productId: Long,
        offset: Int = 0,
        pageSize: Int = DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE,
        includedVariationIds: List<Long> = emptyList(),
        excludedVariationIds: List<Long> = emptyList(),
        filterOptions: Map<VariationFilterOption, String>? = null,
        orderCurrency: String? = null,
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(API, this, "fetchProductVariations") {
            val response = wcProductRestClient.fetchProductVariationsWithSyncRequest(
                site = site,
                productId = productId,
                offset = offset,
                pageSize = pageSize,
                includedVariationIds = includedVariationIds,
                excludedVariationIds = excludedVariationIds,
                filterOptions = filterOptions,
                orderCurrency = orderCurrency
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    if (offset == 0 &&
                        includedVariationIds.isEmpty() &&
                        excludedVariationIds.isEmpty()
                    ) {
                        ProductSqlUtils.deleteVariationsForProduct(site, productId)
                    }

                    ProductSqlUtils.insertOrUpdateProductVariations(response.result)
                    val canLoadMore = response.result.size == pageSize
                    WooResult(canLoadMore)
                }

                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun createVariations(
        site: SiteModel,
        productId: RemoteId,
        variations: List<WCProductVariationModel>,
    ): WooResult<BatchProductVariationsApiResponse> {
        return coroutineEngine.withDefaultContext(API, this, "createVariations") {
            val responses = variations
                .chunked(VARIATIONS_CREATION_LIMIT)
                .map { chunkedVariations ->
                    wcProductRestClient.createVariations(
                        site,
                        productId = productId,
                        variations = chunkedVariations.map {
                            ProductVariationMapper.variantModelToProductJsonBody(
                                variationModel = null,
                                updatedVariationModel = it
                            )
                        }
                    ).asWooResult()
                }
                .onEach { result: WooResult<BatchProductVariationsApiResponse> ->
                    if (!result.isError) {
                        saveVariationsInDatabase(result, productId, site)
                    }
                }

            val anySuccessfulResponse = responses.firstOrNull { !it.isError }
            val anyFailureResponse = responses.firstOrNull { it.isError }

            anySuccessfulResponse
                ?: anyFailureResponse
                ?: WooResult(error = WooError(WooErrorType.GENERIC_ERROR, NETWORK_ERROR))
        }
    }

    suspend fun fetchProductsCount(
        site: SiteModel,
    ): WooResult<Long> {
        return coroutineEngine.withDefaultContext(API, this, "fetchProductsCount") {
            val response = wcProductRestClient.fetchProductsTotals(site)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    WooResult(response.result.sumOf { it.total })
                }

                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    private fun saveVariationsInDatabase(
        result: WooResult<BatchProductVariationsApiResponse>,
        productId: RemoteId,
        site: SiteModel
    ) {
        result.model
            ?.createdVariations
            ?.map { variationResponse ->
                variationResponse.asProductVariationModel().apply {
                    remoteProductId = productId.value
                    localSiteId = site.id
                }
            }
            ?.let { databaseEntities ->
                ProductSqlUtils.insertOrUpdateProductVariations(databaseEntities)
            }
    }

    suspend fun replyToReview(payload: PostReviewReply): WooResult<Unit> {
        return wcProductRestClient.replyToReview(
            site = payload.site,
            productId = payload.productId,
            reviewId = payload.reviewId,
            replyContent = payload.replyContent
        ).asWooResult()
    }

    private fun addProduct(payload: AddProductPayload) {
        with(payload) {
            wcProductRestClient.addProduct(site, product, payload.metadata)
        }
    }

    private fun deleteProduct(payload: DeleteProductPayload) {
        with(payload) {
            wcProductRestClient.deleteProduct(site, remoteProductId, forceDelete)
        }
    }

    private fun extractAddonsFromProductMetaData(metaData: List<WCMetaData>): List<Addon> {
        val remoteAddons = metaData[WCMetaData.AddOnsMetadataKeys.ADDONS_METADATA_KEY]
            ?.let { RemoteAddonDto.fromMetaDataValue(it.value) }

        return remoteAddons.orEmpty()
            .toList()
            .mapNotNull { remoteAddonDto ->
                try {
                    RemoteAddonMapper.toDomain(remoteAddonDto)
                } catch (exception: MappingRemoteException) {
                    logger.e(API, "Exception while parsing $remoteAddonDto: ${exception.message}")
                    null
                }
            }
    }

    private fun handleFetchProductSkuAvailabilityCompleted(payload: RemoteProductSkuAvailabilityPayload) {
        val onProductSkuAvailabilityChanged = OnProductSkuAvailabilityChanged(payload.sku, payload.available)
        if (payload.isError) {
            onProductSkuAvailabilityChanged.also { it.error = payload.error }
        }
        onProductSkuAvailabilityChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_SKU_AVAILABILITY
        emitChange(onProductSkuAvailabilityChanged)
    }

    @Suppress("ForbiddenComment")
    private fun handleFetchProductsCompleted(payload: RemoteProductListPayload) {
        coroutineEngine.launch(T.DB, this, "handleFetchProductsCompleted") {
            val onProductChanged: OnProductChanged

            if (payload.isError) {
                onProductChanged = OnProductChanged(0).also { it.error = payload.error }
            } else {
                // remove the existing products for this site if this is the first page of results
                // or if the remoteProductIds or excludedProductIds are null, otherwise
                // products deleted outside of the app will persist
                if (payload.offset == 0 && payload.remoteProductIds == null && payload.excludedProductIds == null) {
                    productStorageHelper.deleteProductsForSite(payload.site)
                }

                val rowsAffected = productStorageHelper.upsertProducts(payload.productsWithMetaData)
                onProductChanged = OnProductChanged(rowsAffected, canLoadMore = payload.canLoadMore)

                // TODO: 18/08/2021 @wzieba add tests
                coroutineEngine.launch(T.DB, this, "cacheProductsAddons") {
                    payload.productsWithMetaData.forEach { productWithMetaData ->
                        val domainAddons = extractAddonsFromProductMetaData(productWithMetaData.metaData)

                        addonsDao.cacheProductAddons(
                            productRemoteId = productWithMetaData.product.remoteId,
                            localSiteId = payload.site.localId(),
                            addons = domainAddons
                        )
                    }
                }
            }

            onProductChanged.causeOfChange = WCProductAction.FETCH_PRODUCTS
            emitChange(onProductChanged)
        }
    }

    private fun handleSearchProductsCompleted(payload: RemoteSearchProductsPayload) {
        if (payload.isError) {
            emitChange(
                OnProductsSearched(
                    searchQuery = payload.searchQuery,
                    isSkuSearch = payload.skuSearchOptions,
                    globalUniqueIdSearchQuery = payload.globalUniqueIdSearchQuery
                )
            )
        } else {
            coroutineEngine.launch(T.DB, this, "handleSearchProductsCompleted") {
                productStorageHelper.upsertProducts(payload.productsWithMetaData)
                emitChange(
                    OnProductsSearched(
                        searchQuery = payload.searchQuery,
                        isSkuSearch = payload.skuSearchOptions,
                        globalUniqueIdSearchQuery = payload.globalUniqueIdSearchQuery,
                        searchResults = payload.productsWithMetaData.map { it.product },
                        canLoadMore = payload.canLoadMore
                    )
                )
            }
        }
    }

    private fun handleFetchProductShippingClassesCompleted(payload: RemoteProductShippingClassListPayload) {
        val onProductShippingClassesChanged = if (payload.isError) {
            OnProductShippingClassesChanged(0).also { it.error = payload.error }
        } else {
            // delete product shipping class list for site if this is the first page of results, otherwise
            // shipping class list deleted outside of the app will persist
            if (payload.offset == 0) {
                ProductSqlUtils.deleteProductShippingClassListForSite(payload.site)
            }

            val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(payload.shippingClassList)
            OnProductShippingClassesChanged(rowsAffected, canLoadMore = payload.canLoadMore)
        }
        onProductShippingClassesChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_SHIPPING_CLASS_LIST
        emitChange(onProductShippingClassesChanged)
    }

    private fun handleFetchProductShippingClassCompleted(payload: RemoteProductShippingClassPayload) {
        val onProductShippingClassesChanged = if (payload.isError) {
            OnProductShippingClassesChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClass(payload.productShippingClassModel)
            OnProductShippingClassesChanged(rowsAffected)
        }
        onProductShippingClassesChanged.causeOfChange = WCProductAction.FETCH_SINGLE_PRODUCT_SHIPPING_CLASS
        emitChange(onProductShippingClassesChanged)
    }

    private fun handleFetchProductPasswordCompleted(payload: RemoteProductPasswordPayload) {
        val onProductPasswordChanged = if (payload.isError) {
            OnProductPasswordChanged(payload.remoteProductId, "").also { it.error = payload.error }
        } else {
            OnProductPasswordChanged(payload.remoteProductId, payload.password)
        }
        onProductPasswordChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_PASSWORD
        emitChange(onProductPasswordChanged)
    }

    private fun handleUpdatedProductPasswordCompleted(payload: RemoteUpdatedProductPasswordPayload) {
        val onProductPasswordUpdated = if (payload.isError) {
            OnProductPasswordChanged(payload.remoteProductId, null).also { it.error = payload.error }
        } else {
            OnProductPasswordChanged(payload.remoteProductId, payload.password)
        }
        onProductPasswordUpdated.causeOfChange = WCProductAction.UPDATE_PRODUCT_PASSWORD
        emitChange(onProductPasswordUpdated)
    }

    private fun handleUpdateProductImages(payload: RemoteUpdateProductImagesPayload) {
        coroutineEngine.launch(T.DB, this, "handleUpdateProductImages") {
            val onProductImagesChanged: OnProductImagesChanged

            if (payload.isError) {
                onProductImagesChanged = OnProductImagesChanged(
                    0,
                    payload.productWithMetaData.product.remoteProductId
                ).also {
                    it.error = payload.error
                }
            } else {
                val rowsAffected = productStorageHelper.upsertProduct(payload.productWithMetaData)
                onProductImagesChanged = OnProductImagesChanged(
                    rowsAffected,
                    payload.productWithMetaData.product.remoteProductId
                )
            }

            onProductImagesChanged.causeOfChange = WCProductAction.UPDATED_PRODUCT_IMAGES
            emitChange(onProductImagesChanged)
        }
    }

    private fun handleUpdateProduct(payload: RemoteUpdateProductPayload) {
        coroutineEngine.launch(T.DB, this, "handleUpdateProduct") {
            val onProductUpdated: OnProductUpdated

            if (payload.isError) {
                onProductUpdated = OnProductUpdated(0, payload.productWithMetaData.product.remoteProductId)
                    .also { it.error = payload.error }
            } else {
                val rowsAffected = productStorageHelper.upsertProduct(payload.productWithMetaData)
                onProductUpdated = OnProductUpdated(rowsAffected, payload.productWithMetaData.product.remoteProductId)
            }

            onProductUpdated.causeOfChange = WCProductAction.UPDATED_PRODUCT
            emitChange(onProductUpdated)
        }
    }

    private fun handleFetchProductCategories(payload: RemoteProductCategoriesPayload) {
        coroutineEngine.launch(T.DB, this, "handleFetchProductCategories") {
            val onProductCategoryChanged: OnProductCategoryChanged

            if (payload.isError) {
                onProductCategoryChanged = OnProductCategoryChanged(0).also { it.error = payload.error }
            } else {
                // Clear existing product categories if this is a fresh fetch (loadMore = false).
                // This is the simplest way to keep our local categories in sync with remote categories
                // in case of deletions.
                if (!payload.loadedMore) {
                    ProductSqlUtils.deleteAllProductCategoriesForSite(payload.site)
                }
                val rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(
                    payload.categories
                )
                onProductCategoryChanged = OnProductCategoryChanged(
                    rowsAffected,
                    canLoadMore = payload.canLoadMore
                )
            }

            onProductCategoryChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_CATEGORIES
            emitChange(onProductCategoryChanged)
        }
    }

    private fun handleAddProductCategory(payload: RemoteAddProductCategoryResponsePayload) {
        coroutineEngine.launch(T.DB, this, "handleAddProductCategory") {
            val onProductCategoryChanged: OnProductCategoryChanged

            if (payload.isError) {
                onProductCategoryChanged = OnProductCategoryChanged(0).also { it.error = payload.error }
            } else {
                val rowsAffected = payload.category?.let {
                    ProductSqlUtils.insertOrUpdateProductCategory(it)
                } ?: 0
                onProductCategoryChanged = OnProductCategoryChanged(rowsAffected)
            }

            onProductCategoryChanged.causeOfChange = WCProductAction.ADDED_PRODUCT_CATEGORY
            emitChange(onProductCategoryChanged)
        }
    }

    private fun handleFetchProductTagsCompleted(payload: RemoteProductTagsPayload) {
        val onProductTagsChanged = if (payload.isError) {
            OnProductTagChanged(0).also { it.error = payload.error }
        } else {
            // delete product tags for site if this is the first page of results, otherwise
            // tags deleted outside of the app will persist
            if (payload.offset == 0 && payload.searchQuery.isNullOrEmpty()) {
                ProductSqlUtils.deleteProductTagsForSite(payload.site)
            }

            val rowsAffected = ProductSqlUtils.insertOrUpdateProductTags(payload.tags)
            OnProductTagChanged(rowsAffected, canLoadMore = payload.canLoadMore)
        }
        onProductTagsChanged.causeOfChange = WCProductAction.FETCH_PRODUCT_TAGS
        emitChange(onProductTagsChanged)
    }

    private fun handleAddProductTags(payload: RemoteAddProductTagsResponsePayload) {
        val onProductTagsChanged: OnProductTagChanged
        if (payload.isError) {
            onProductTagsChanged = OnProductTagChanged(0).also { it.error = payload.error }
        } else {
            val rowsAffected = ProductSqlUtils.insertOrUpdateProductTags(payload.tags.filter { it.name.isNotEmpty() })
            onProductTagsChanged = OnProductTagChanged(rowsAffected)
        }

        onProductTagsChanged.causeOfChange = WCProductAction.ADDED_PRODUCT_TAGS
        emitChange(onProductTagsChanged)
    }

    private fun handleAddNewProduct(payload: RemoteAddProductPayload) {
        coroutineEngine.launch(T.DB, this, "handleAddNewProduct") {
            val onProductCreated: OnProductCreated

            if (payload.isError) {
                onProductCreated = OnProductCreated(
                    0,
                    payload.productWithMetaData.product.remoteProductId
                ).also { it.error = payload.error }
            } else {
                val rowsAffected = productStorageHelper.upsertProduct(payload.productWithMetaData)
                onProductCreated = OnProductCreated(rowsAffected, payload.productWithMetaData.product.remoteProductId)
            }

            onProductCreated.causeOfChange = WCProductAction.ADDED_PRODUCT
            emitChange(onProductCreated)
        }
    }

    private fun handleDeleteProduct(payload: RemoteDeleteProductPayload) {
        coroutineEngine.launch(T.DB, this, "handleDeleteProduct") {
            val onProductChanged: OnProductChanged

            if (payload.isError) {
                onProductChanged = OnProductChanged(0).also { it.error = payload.error }
            } else {
                val rowsAffected = productStorageHelper.deleteProduct(
                    payload.site,
                    payload.remoteProductId
                )
                onProductChanged = OnProductChanged(rowsAffected, payload.remoteProductId)
            }

            onProductChanged.causeOfChange = WCProductAction.DELETED_PRODUCT
            emitChange(onProductChanged)
        }
    }

    suspend fun getProductWithMetaData(
        site: SiteModel,
        remoteProductId: Long
    ) = productStorageHelper.getProduct(site, remoteProductId)

    suspend fun getProductMetaData(
        site: SiteModel,
        remoteProductId: Long
    ) = productStorageHelper.getProductMetadata(site = site, remoteProductId = remoteProductId)

    data class ProductSearchResult(
        val products: List<WCProductModel>,
        val canLoadMore: Boolean
    )

    data class ProductCategorySearchResult(
        val categories: List<WCProductCategoryModel>,
        val canLoadMore: Boolean
    )
}
