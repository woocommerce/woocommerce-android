package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.StripProductVariationMetaData
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.model.metadata.MetadataChanges
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostWPComRestResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductVariationMapper.variantModelToProductJsonBody
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_CATEGORY_SORTING
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_SORTING
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_TAGS_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductReviewsResponsePayload
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting.NAME_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType.TERM_EXISTS
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteAddProductTagsResponsePayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteDeleteProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductCategoriesPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductListPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPasswordPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductReviewPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassListPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductShippingClassPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductSkuAvailabilityPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductTagsPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteProductVariationsPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteSearchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductImagesPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdateVariationPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteUpdatedProductPasswordPayload
import org.wordpress.android.fluxc.store.WCProductStore.RemoteVariationPayload
import org.wordpress.android.fluxc.store.WCProductStore.SkuSearchOptions
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.putIfNotEmpty
import org.wordpress.android.fluxc.utils.putIfNotNull
import org.wordpress.android.fluxc.utils.toWooPayload
import org.wordpress.android.util.AppLog
import javax.inject.Inject

@Suppress("LargeClass")
class ProductRestClient @Inject constructor(
    private val dispatcher: Dispatcher,
    private val wooNetwork: WooNetwork,
    private val wpComNetwork: WPComNetwork,
    private val coroutineEngine: CoroutineEngine,
    private val stripProductVariationMetaData: StripProductVariationMetaData,
    private val productDtoMapper: ProductDtoMapper
) {
    /**
     * Makes a GET request to `/wp-json/wc/v3/products/shipping_classes/[remoteShippingClassId]`
     * to fetch a single product shipping class
     *
     * Dispatches a WCProductAction.FETCHED_SINGLE_PRODUCT_SHIPPING_CLASS action with the result
     *
     * @param [remoteShippingClassId] Unique server id of the shipping class to fetch
     */
    fun fetchSingleProductShippingClass(site: SiteModel, remoteShippingClassId: Long) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchSingleProductShippingClass") {
            val url = WOOCOMMERCE.products.shipping_classes.id(remoteShippingClassId).pathV3
            val params = emptyMap<String, String>()
            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                params = params,
                clazz = ProductShippingClassApiResponse::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    response.data?.let {
                        val newModel = productShippingClassResponseToProductShippingClassModel(
                            it, site
                        ).apply { localSiteId = site.id }
                        val payload = RemoteProductShippingClassPayload(newModel, site)
                        dispatcher.dispatch(
                            WCProductActionBuilder.newFetchedSingleProductShippingClassAction(
                                payload
                            )
                        )
                    }
                }

                is WPAPIResponse.Error -> {
                    val productError = wpAPINetworkErrorToProductError(response.error)
                    val payload = RemoteProductShippingClassPayload(
                        productError,
                        WCProductShippingClassModel().apply { this.remoteShippingClassId = remoteShippingClassId },
                        site
                    )
                    dispatcher.dispatch(
                        WCProductActionBuilder.newFetchedSingleProductShippingClassAction(
                            payload
                        )
                    )
                }
            }
        }
    }

    /**
     * Makes a GET request to `GET /wp-json/wc/v3/products/shipping_classes` to fetch
     * product shipping classes for a site
     *
     * Dispatches a WCProductAction.FETCHED_PRODUCT_SHIPPING_CLASS_LIST action with the result
     *
     * @param [site] The site to fetch product shipping class list for
     */
    fun fetchProductShippingClassList(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE,
        offset: Int = 0
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchProductShippingClassList") {
            val url = WOOCOMMERCE.products.shipping_classes.pathV3
            val params = mutableMapOf(
                "per_page" to pageSize.toString(),
                "offset" to offset.toString()
            )

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                params = params,
                clazz = Array<ProductShippingClassApiResponse>::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val shippingClassList = response.data?.map {
                        productShippingClassResponseToProductShippingClassModel(it, site)
                    }.orEmpty()

                    val loadedMore = offset > 0
                    val canLoadMore = shippingClassList.size == pageSize
                    val payload = RemoteProductShippingClassListPayload(
                        site, shippingClassList, offset, loadedMore, canLoadMore
                    )
                    dispatcher.dispatch(
                        WCProductActionBuilder.newFetchedProductShippingClassListAction(
                            payload
                        )
                    )
                }

                is WPAPIResponse.Error -> {
                    val productError = wpAPINetworkErrorToProductError(response.error)
                    val payload = RemoteProductShippingClassListPayload(productError, site)
                    dispatcher.dispatch(
                        WCProductActionBuilder.newFetchedProductShippingClassListAction(
                            payload
                        )
                    )
                }
            }
        }
    }

    /**
     * Makes a GET request to `GET /wp-json/wc/v3/products/tags` to fetch
     * product tags for a site
     *
     * Dispatches a WCProductAction.FETCHED_PRODUCT_TAGS action with the result
     *
     * @param [site] The site to fetch product shipping class list for
     * @param [pageSize] The size of the tags needed from the API response
     * @param [offset] The page number passed to the API
     */
    fun fetchProductTags(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_TAGS_PAGE_SIZE,
        offset: Int = 0,
        searchQuery: String? = null
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchProductTags") {
            val url = WOOCOMMERCE.products.tags.pathV3
            val params = mutableMapOf(
                "per_page" to pageSize.toString(),
                "offset" to offset.toString()
            ).putIfNotEmpty("search" to searchQuery)

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                clazz = Array<ProductTagApiResponse>::class.java,
                params = params
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val tags = response.data?.map {
                        productTagApiResponseToProductTagModel(it, site)
                    }.orEmpty()

                    val loadedMore = offset > 0
                    val canLoadMore = tags.size == pageSize
                    val payload = RemoteProductTagsPayload(
                        site,
                        tags,
                        offset,
                        loadedMore,
                        canLoadMore,
                        searchQuery
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductTagsAction(payload))
                }

                is WPAPIResponse.Error -> {
                    val productError = wpAPINetworkErrorToProductError(response.error)
                    val payload = RemoteProductTagsPayload(productError, site)
                    dispatcher.dispatch(WCProductActionBuilder.newFetchedProductTagsAction(payload))
                }
            }
        }
    }

    /**
     * Makes a POST request to `POST /wp-json/wc/v3/products/tags/batch` to add
     * product tags for a site
     *
     * Dispatches a [WCProductAction.ADDED_PRODUCT_TAGS] action with the result
     *
     * @param [site] The site to fetch product shipping class list for
     * @param [tags] The list of tag names that needed to be added to the site
     */
    fun addProductTags(
        site: SiteModel,
        tags: List<String>
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "addProductTags") {
            val url = WOOCOMMERCE.products.tags.batch.pathV3
            val body = mutableMapOf(
                "create" to tags.map { mapOf("name" to it) }
            )

            val response = wooNetwork.executePostGsonRequest(
                site = site,
                path = url,
                body = body,
                clazz = BatchAddProductTagApiResponse::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val addedTags = response.data?.addedTags?.map {
                        productTagApiResponseToProductTagModel(it, site)
                    }.orEmpty()

                    val payload = RemoteAddProductTagsResponsePayload(site, addedTags)
                    dispatcher.dispatch(WCProductActionBuilder.newAddedProductTagsAction(payload))
                }

                is WPAPIResponse.Error -> {
                    val productError = wpAPINetworkErrorToProductError(response.error)
                    val payload = RemoteAddProductTagsResponsePayload(productError, site)
                    dispatcher.dispatch(WCProductActionBuilder.newAddedProductTagsAction(payload))
                }
            }
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products/[remoteProductId]` to fetch a single product
     *
     * Dispatches a WCProductAction.FETCHED_SINGLE_PRODUCT action with the result
     *
     * @param [remoteProductId] Unique server id of the product to fetch
     */
    suspend fun fetchSingleProduct(site: SiteModel, remoteProductId: Long): RemoteProductPayload {
        val url = WOOCOMMERCE.products.id(remoteProductId).pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = emptyMap(),
            clazz = ProductApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    val newModel = productDtoMapper.mapToModel(site.localId(), it)
                    RemoteProductPayload(newModel, site)
                } ?: RemoteProductPayload(
                    ProductError(GENERIC_ERROR, "Success response with empty data"),
                    ProductWithMetaData(WCProductModel().apply { this.remoteProductId = remoteProductId }),
                    site
                )
            }

            is WPAPIResponse.Error -> {
                val productError = wpAPINetworkErrorToProductError(response.error)
                RemoteProductPayload(
                    productError,
                    ProductWithMetaData(WCProductModel().apply { this.remoteProductId = remoteProductId }),
                    site
                )
            }
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products/[remoteProductId]/variations/[remoteVariationId]` to fetch
     * a single product variation
     *
     *
     * @param [remoteProductId] Unique server id of the product to fetch
     * @param [remoteVariationId] Unique server id of the variation to fetch
     */
    suspend fun fetchSingleVariation(
        site: SiteModel,
        remoteProductId: Long,
        remoteVariationId: Long
    ): RemoteVariationPayload {
        val url = WOOCOMMERCE.products.id(remoteProductId).variations.variation(remoteVariationId).pathV3
        val params = emptyMap<String, String>()

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = ProductVariationApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                val productData = response.data
                if (productData != null) {
                    RemoteVariationPayload(
                        productData.asProductVariationModel().apply {
                            this.remoteProductId = remoteProductId
                            localSiteId = site.id
                            metadata = stripProductVariationMetaData(metadata)
                        },
                        site
                    )
                } else {
                    RemoteVariationPayload(
                        ProductError(GENERIC_ERROR, "Success response with empty data"),
                        WCProductVariationModel().apply {
                            this.remoteProductId = remoteProductId
                            this.remoteVariationId = remoteVariationId
                        },
                        site
                    )
                }
            }

            is WPAPIResponse.Error -> {
                RemoteVariationPayload(
                    wpAPINetworkErrorToProductError(response.error),
                    WCProductVariationModel().apply {
                        this.remoteProductId = remoteProductId
                        this.remoteVariationId = remoteVariationId
                    },
                    site
                )
            }
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products` retrieving a list of products for the given
     * WooCommerce [SiteModel].
     *
     * Dispatches a [WCProductAction.FETCHED_PRODUCTS] action with the resulting list of products.
     */
    @Suppress("LongMethod")
    fun fetchProducts(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        offset: Int = 0,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        searchQuery: String? = null,
        skuSearchOptions: SkuSearchOptions = SkuSearchOptions.Disabled,
        globalUniqueIdSearchQuery: String? = null,
        includedProductIds: List<Long>? = null,
        filterOptions: Map<ProductFilterOption, String>? = null,
        excludedProductIds: List<Long>? = null
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchProducts") {
            val url = WOOCOMMERCE.products.pathV3
            val params = buildProductParametersMap(
                pageSize = pageSize,
                sortType = sortType,
                offset = offset,
                searchQuery = searchQuery,
                skuSearchOptions = skuSearchOptions,
                globalUniqueIdSearchQuery = globalUniqueIdSearchQuery,
                includedProductIds = includedProductIds,
                excludedProductIds = excludedProductIds,
                filterOptions = filterOptions
            )

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                params = params,
                clazz = Array<ProductApiResponse>::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val productModels = response.data?.map {
                        productDtoMapper.mapToModel(site.localId(), it)
                    }.orEmpty()

                    val loadedMore = offset > 0
                    val canLoadMore = productModels.size == pageSize
                    if (searchQuery == null && globalUniqueIdSearchQuery == null) {
                        val payload = RemoteProductListPayload(
                            site,
                            productModels,
                            offset,
                            loadedMore,
                            canLoadMore,
                            includedProductIds,
                            excludedProductIds
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedProductsAction(payload))
                    } else {
                        val payload = RemoteSearchProductsPayload(
                            site = site,
                            searchQuery = searchQuery,
                            globalUniqueIdSearchQuery = globalUniqueIdSearchQuery,
                            skuSearchOptions = skuSearchOptions,
                            productsWithMetaData = productModels,
                            offset = offset,
                            loadedMore = loadedMore,
                            canLoadMore = canLoadMore
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newSearchedProductsAction(payload))
                    }
                }

                is WPAPIResponse.Error -> {
                    val productError = wpAPINetworkErrorToProductError(response.error)
                    if (searchQuery == null) {
                        val payload = RemoteProductListPayload(productError, site)
                        dispatcher.dispatch(WCProductActionBuilder.newFetchedProductsAction(payload))
                    } else {
                        val payload = RemoteSearchProductsPayload(
                            error = productError,
                            site = site,
                            query = searchQuery,
                            skuSearchOptions = skuSearchOptions,
                            globalUniqueIdSearchQuery = globalUniqueIdSearchQuery,
                            filterOptions = filterOptions
                        )
                        dispatcher.dispatch(WCProductActionBuilder.newSearchedProductsAction(payload))
                    }
                }
            }
        }
    }

    fun searchProductsByGlobalUniqueId(
        site: SiteModel,
        globalUniqueIdSearchQuery: String? = null,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        offset: Int = 0,
        sorting: ProductSorting = DEFAULT_PRODUCT_SORTING,
        excludedProductIds: List<Long>? = null,
        filterOptions: Map<ProductFilterOption, String>? = null
    ) {
        fetchProducts(
            site = site,
            pageSize = pageSize,
            offset = offset,
            sortType = sorting,
            globalUniqueIdSearchQuery = globalUniqueIdSearchQuery,
            excludedProductIds = excludedProductIds,
            filterOptions = filterOptions
        )
    }

    fun searchProducts(
        site: SiteModel,
        searchQuery: String,
        skuSearchOptions: SkuSearchOptions = SkuSearchOptions.Disabled,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        offset: Int = 0,
        sorting: ProductSorting = DEFAULT_PRODUCT_SORTING,
        excludedProductIds: List<Long>? = null,
        filterOptions: Map<ProductFilterOption, String>? = null
    ) {
        fetchProducts(
            site = site,
            pageSize = pageSize,
            offset = offset,
            sortType = sorting,
            searchQuery = searchQuery,
            skuSearchOptions = skuSearchOptions,
            excludedProductIds = excludedProductIds,
            filterOptions = filterOptions
        )
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products` retrieving a list of products for the given
     * WooCommerce [SiteModel].
     *
     * but requiring this call to be suspended so the return call be synced within the coroutine job
     */
    suspend fun fetchProductsWithSyncRequest(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_PAGE_SIZE,
        offset: Int = 0,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        includedProductIds: List<Long>? = null,
        excludedProductIds: List<Long>? = null,
        searchQuery: String? = null,
        skuSearchOptions: SkuSearchOptions = SkuSearchOptions.Disabled,
        filterOptions: Map<ProductFilterOption, String>? = null,
        includeTypes: List<WCProductStore.IncludeType> = emptyList()
    ): WooPayload<List<ProductWithMetaData>> {
        val params = buildProductParametersMap(
            pageSize = pageSize,
            sortType = sortType,
            offset = offset,
            searchQuery = searchQuery,
            skuSearchOptions = skuSearchOptions,
            includedProductIds = includedProductIds,
            excludedProductIds = excludedProductIds,
            filterOptions = filterOptions,
            includeTypes = includeTypes,
        )

        val url = WOOCOMMERCE.products.pathV3
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<ProductApiResponse>::class.java
        )

        return response.toWooPayload { products ->
            products.map {
                productDtoMapper.mapToModel(site.localId(), it)
            }
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/reports/products/totals/` retrieving count of products for the given sute
     */
    suspend fun fetchProductsTotals(site: SiteModel): WooPayload<List<ProductCountApiResponse>> {
        val url = WOOCOMMERCE.reports.products.totals.pathV3
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = emptyMap(),
            clazz = Array<ProductCountApiResponse>::class.java
        )

        return response.toWooPayload { it.toList() }
    }

    private fun buildProductParametersMap(
        pageSize: Int,
        sortType: ProductSorting,
        offset: Int,
        searchQuery: String?,
        skuSearchOptions: SkuSearchOptions,
        globalUniqueIdSearchQuery: String? = null,
        includedProductIds: List<Long>? = null,
        excludedProductIds: List<Long>? = null,
        filterOptions: Map<ProductFilterOption, String>? = null,
        includeTypes: List<WCProductStore.IncludeType> = emptyList()
    ): MutableMap<String, String> {
        fun ProductSorting.asOrderByParameter() = when (this) {
            TITLE_ASC, TITLE_DESC -> "title"
            DATE_ASC, DATE_DESC -> "date"
        }

        fun ProductSorting.asSortOrderParameter() = when (this) {
            TITLE_ASC, DATE_ASC -> "asc"
            TITLE_DESC, DATE_DESC -> "desc"
        }

        val params = mutableMapOf(
            "per_page" to pageSize.toString(),
            "orderby" to sortType.asOrderByParameter(),
            "order" to sortType.asSortOrderParameter(),
            "offset" to offset.toString(),
            "include_types" to includeTypes.joinToString(",") { it.value }
        )

        includedProductIds?.let { includedIds ->
            params.putIfNotEmpty("include" to includedIds.map { it }.joinToString())
        }
        excludedProductIds?.let { excludedIds ->
            params.putIfNotEmpty("exclude" to excludedIds.map { it }.joinToString())
        }
        filterOptions?.let { options ->
            params.putAll(options.map { it.key.toString() to it.value })
        }

        if (searchQuery.isNullOrEmpty().not()) {
            when (skuSearchOptions) {
                SkuSearchOptions.Disabled -> {
                    params["search"] = searchQuery!!
                }

                SkuSearchOptions.ExactSearch -> {
                    params["sku"] = searchQuery!! // full SKU match
                }

                SkuSearchOptions.PartialMatch -> {
                    params["sku"] = searchQuery!! // full SKU match
                    params["search_sku"] = searchQuery // partial SKU match, added in core v6.6
                }
            }
        }

        addGlobalUniqueIdSearchQuery(params, globalUniqueIdSearchQuery)

        return params
    }

    private fun addGlobalUniqueIdSearchQuery(
        params: MutableMap<String, String>,
        globalUniqueIdSearchQuery: String?
    ) {
        if (!globalUniqueIdSearchQuery.isNullOrEmpty()) {
            params["global_unique_id"] = globalUniqueIdSearchQuery
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products/categories` retrieving a list of product
     * categories for the given WooCommerce [SiteModel].
     *
     * but requiring this call to be suspended so the return call be synced within the coroutine job
     */
    suspend fun fetchProductsCategoriesWithSyncRequest(
        site: SiteModel,
        includedCategoryIds: List<Long> = emptyList(),
        excludedCategoryIds: List<Long> = emptyList(),
        searchQuery: String? = null,
        pageSize: Int = DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE,
        productCategorySorting: ProductCategorySorting = DEFAULT_CATEGORY_SORTING,
        offset: Int = 0
    ): WooPayload<List<WCProductCategoryModel>> {
        val sortOrder = when (productCategorySorting) {
            NAME_DESC -> "desc"
            else -> "asc"
        }

        val params = mutableMapOf(
            "per_page" to pageSize.toString(),
            "offset" to offset.toString(),
            "order" to sortOrder,
            "orderby" to "name"
        ).putIfNotEmpty("search" to searchQuery)
        if (includedCategoryIds.isNotEmpty()) {
            params["include"] = includedCategoryIds.map { it }.joinToString()
        }
        if (excludedCategoryIds.isNotEmpty()) {
            params["exclude"] = excludedCategoryIds.map { it }.joinToString()
        }

        val url = WOOCOMMERCE.products.categories.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<ProductCategoryApiResponse>::class.java
        )

        return response.toWooPayload { categories ->
            categories.map {
                it.asProductCategoryModel()
                    .apply { localSiteId = site.id }
            }
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products` for a given [SiteModel] and [sku] to check
     * if this [sku] already exists on the site
     *
     * Dispatches a [WCProductAction.FETCHED_PRODUCT_SKU_AVAILABILITY] action with the availability for the [sku].
     */
    fun fetchProductSkuAvailability(
        site: SiteModel,
        sku: String
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchProductSkuAvailability") {
            val url = WOOCOMMERCE.products.pathV3
            val params = mutableMapOf("sku" to sku, "_fields" to "sku")

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                params = params,
                clazz = Array<ProductApiResponse>::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val available = response.data?.isEmpty() ?: false
                    val payload = RemoteProductSkuAvailabilityPayload(site, sku, available)
                    dispatcher.dispatch(
                        WCProductActionBuilder.newFetchedProductSkuAvailabilityAction(
                            payload
                        )
                    )
                }

                is WPAPIResponse.Error -> {
                    val productError = wpAPINetworkErrorToProductError(response.error)
                    // If there is a network error of some sort that prevents us from knowing if a sku is available
                    // then just consider sku as available
                    val payload = RemoteProductSkuAvailabilityPayload(productError, site, sku, true)
                    dispatcher.dispatch(
                        WCProductActionBuilder.newFetchedProductSkuAvailabilityAction(
                            payload
                        )
                    )
                }
            }
        }
    }

    /**
     * Makes a WP.COM GET request to `/sites/$site/posts/$post_ID` to fetch just the password for a product
     */
    fun fetchProductPassword(site: SiteModel, remoteProductId: Long) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchProductPassword") {
            val url = WPCOMREST.sites.site(site.siteId).posts.post(remoteProductId).urlV1_1
            val params = mutableMapOf("fields" to "password")

            val response = wpComNetwork.executeGetGsonRequest(
                url = url,
                params = params,
                clazz = PostWPComRestResponse::class.java
            )

            when (response) {
                is WPComGsonRequestBuilder.Response.Success -> {
                    val payload = RemoteProductPasswordPayload(
                        remoteProductId,
                        site,
                        response.data.password ?: ""
                    )
                    dispatcher.dispatch(
                        WCProductActionBuilder.newFetchedProductPasswordAction(
                            payload
                        )
                    )
                }

                is WPComGsonRequestBuilder.Response.Error -> {
                    val payload = RemoteProductPasswordPayload(remoteProductId, site, "")
                    payload.error = networkErrorToProductError(response.error)
                    dispatcher.dispatch(
                        WCProductActionBuilder.newFetchedProductPasswordAction(
                            payload
                        )
                    )
                }
            }
        }
    }

    /**
     * Makes a WP.COM POST request to `/sites/$site/posts/$post_ID` to update just the password for a product
     */
    fun updateProductPassword(site: SiteModel, remoteProductId: Long, password: String) {
        coroutineEngine.launch(AppLog.T.API, this, "updateProductPassword") {
            val url = WPCOMREST.sites.site(site.siteId).posts.post(remoteProductId).urlV1_2
            val params = mapOf(
                "context" to "edit",
                "fields" to "password"
            )
            val body = mapOf(
                "password" to password
            )

            val response = wpComNetwork.executePostGsonRequest(
                url = url,
                params = params,
                body = body,
                clazz = PostWPComRestResponse::class.java
            )

            when (response) {
                is WPComGsonRequestBuilder.Response.Success -> {
                    val payload = RemoteUpdatedProductPasswordPayload(
                        remoteProductId,
                        site,
                        response.data.password ?: ""
                    )
                    dispatcher.dispatch(
                        WCProductActionBuilder.newUpdatedProductPasswordAction(
                            payload
                        )
                    )
                }

                is WPComGsonRequestBuilder.Response.Error -> {
                    val payload = RemoteUpdatedProductPasswordPayload(remoteProductId, site, "")
                    payload.error = networkErrorToProductError(response.error)
                    dispatcher.dispatch(
                        WCProductActionBuilder.newUpdatedProductPasswordAction(
                            payload
                        )
                    )
                }
            }
        }
    }

    /**
     * Makes a GET request to `POST /wp-json/wc/v3/products/[productId]/variations` to fetch
     * variations for a product
     *
     * @param [productId] Unique server id of the product
     *
     */
    suspend fun fetchProductVariations(
        site: SiteModel,
        productId: Long,
        pageSize: Int = DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE,
        offset: Int = 0
    ): RemoteProductVariationsPayload {
        val url = WOOCOMMERCE.products.id(productId).variations.pathV3
        val params = mutableMapOf(
            "per_page" to pageSize.toString(),
            "offset" to offset.toString(),
            "order" to "asc",
            "orderby" to "menu_order"
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<ProductVariationApiResponse>::class.java
        )

        when (response) {
            is WPAPIResponse.Success -> {
                val variationModels = response.data?.map {
                    it.asProductVariationModel().apply {
                        localSiteId = site.id
                        remoteProductId = productId
                        metadata = stripProductVariationMetaData(metadata)
                    }
                }.orEmpty()

                val loadedMore = offset > 0
                val canLoadMore = variationModels.size == pageSize
                return RemoteProductVariationsPayload(
                    site, productId, variationModels, offset, loadedMore, canLoadMore
                )
            }

            is WPAPIResponse.Error -> {
                val productError = wpAPINetworkErrorToProductError(response.error)
                return RemoteProductVariationsPayload(
                    productError,
                    site,
                    productId
                )
            }
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products/[productId]/variations` retrieving a list of
     * variations for the given WooCommerce [SiteModel] and product.
     *
     * @param [productId] Unique server id of the product
     *
     * but requiring this call to be suspended so the return call be synced within the coroutine job
     *
     */
    suspend fun fetchProductVariationsWithSyncRequest(
        site: SiteModel,
        productId: Long,
        pageSize: Int,
        offset: Int,
        includedVariationIds: List<Long> = emptyList(),
        searchQuery: String? = null,
        excludedVariationIds: List<Long> = emptyList(),
        filterOptions: Map<WCProductStore.VariationFilterOption, String>? = null,
    ): WooPayload<List<WCProductVariationModel>> {
        val params = mutableMapOf(
            "per_page" to pageSize.toString(),
            "orderby" to "menu_order",
            "order" to "asc",
            "offset" to offset.toString()
        ).putIfNotEmpty("search" to searchQuery)
            .putIfNotEmpty("include" to includedVariationIds.map { it }.joinToString())
            .putIfNotEmpty("exclude" to excludedVariationIds.map { it }.joinToString())

        filterOptions?.let { options ->
            params.putAll(options.map { it.key.toString() to it.value })
        }

        val url = WOOCOMMERCE.products.id(productId).variations.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<ProductVariationApiResponse>::class.java
        )

        return response.toWooPayload { variations ->
            variations.map {
                it.asProductVariationModel()
                    .apply {
                        localSiteId = site.id
                        remoteProductId = productId
                        metadata = stripProductVariationMetaData(metadata)
                    }
            }
        }
    }

    /**
     * Makes a PUT request to `/wp-json/wc/v3/products/remoteProductId` to update a product
     *
     * Dispatches a WCProductAction.UPDATED_PRODUCT action with the result
     *
     * @param [site] The site to fetch product reviews for
     * @param [storedWCProductModel] the stored model to compare with the [updatedProductModel]
     * @param [updatedProductModel] the product model that contains the update
     * @param [metadataChanges] the metadata changes to be updated
     */
    fun updateProduct(
        site: SiteModel,
        storedWCProductModel: WCProductModel?,
        updatedProductModel: WCProductModel,
        metadataChanges: MetadataChanges? = null
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "updateProduct") {
            val remoteProductId = updatedProductModel.remoteProductId
            val url = WOOCOMMERCE.products.id(remoteProductId).pathV3
            val body = productModelToProductJsonBody(
                productModel = storedWCProductModel,
                updatedProductModel = updatedProductModel,
                metadata = metadataChanges?.toJsonArray()
            )

            val response = wooNetwork.executePutGsonRequest(
                site = site,
                path = url,
                body = body,
                clazz = ProductApiResponse::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    response.data?.let {
                        val newModel = productDtoMapper.mapToModel(site.localId(), it)
                        val payload = RemoteUpdateProductPayload(site, newModel)
                        dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductAction(payload))
                    }
                }

                is WPAPIResponse.Error -> {
                    val productError = wpAPINetworkErrorToProductError(response.error)
                    val payload = RemoteUpdateProductPayload(
                        productError,
                        site,
                        ProductWithMetaData(WCProductModel().apply { this.remoteProductId = remoteProductId })
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductAction(payload))
                }
            }
        }
    }

    /**
     * Makes a PUT request to `/wp-json/wc/v3/products/remoteProductId` to update a product
     *
     * @param [site] The site to fetch product reviews for
     * @param [storedWCProductVariationModel] the stored model to compare with the [updatedProductVariationModel]
     * @param [updatedProductVariationModel] the product model that contains the update
     */
    suspend fun updateVariation(
        site: SiteModel,
        storedWCProductVariationModel: WCProductVariationModel?,
        updatedProductVariationModel: WCProductVariationModel
    ): RemoteUpdateVariationPayload {
        val remoteProductId = updatedProductVariationModel.remoteProductId
        val remoteVariationId = updatedProductVariationModel.remoteVariationId
        val url = WOOCOMMERCE.products.id(remoteProductId).variations.variation(remoteVariationId).pathV3
        val body = variantModelToProductJsonBody(
            storedWCProductVariationModel,
            updatedProductVariationModel
        )

        val response = wooNetwork.executePutGsonRequest(
            site = site,
            path = url,
            body = body,
            clazz = ProductVariationApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    val newModel = it.asProductVariationModel().apply {
                        this.remoteProductId = remoteProductId
                        localSiteId = site.id
                        metadata = stripProductVariationMetaData(metadata)
                    }
                    RemoteUpdateVariationPayload(site, newModel)
                } ?: RemoteUpdateVariationPayload(
                    ProductError(GENERIC_ERROR, "Success response with empty data"),
                    site,
                    WCProductVariationModel().apply {
                        this.remoteProductId = remoteProductId
                        this.remoteVariationId = remoteVariationId
                    }
                )
            }

            is WPAPIResponse.Error -> {
                val productError = wpAPINetworkErrorToProductError(response.error)
                RemoteUpdateVariationPayload(
                    productError,
                    site,
                    WCProductVariationModel().apply {
                        this.remoteProductId = remoteProductId
                        this.remoteVariationId = remoteVariationId
                    }
                )
            }
        }
    }

    suspend fun batchUpdateProducts(
        site: SiteModel,
        existingToUpdatedProducts: Map<WCProductModel, WCProductModel>,
    ): WooPayload<List<ProductWithMetaData>?> = WOOCOMMERCE.products.batch.pathV3
        .let { url ->
            val body = buildMap {
                putIfNotNull("update" to existingToUpdatedProducts
                    .mapNotNull { (existing, updated) ->
                        productModelToProductJsonBody(
                            productModel = existing, updatedProductModel = updated
                        ).let { updateProperties ->
                            if (updateProperties.isNotEmpty()) {
                                updateProperties.plus("id" to updated.remoteProductId)
                            } else {
                                null
                            }
                        }
                    }
                    .takeIf { map -> map.isNotEmpty() }
                )
            }

            // No changes need to be executed, no need to call the API
            if (body.isEmpty()) return@let WooPayload()

            wooNetwork.executePostGsonRequest(
                site = site,
                path = url,
                clazz = BatchProductApiResponse::class.java,
                body = body
            ).toWooPayload { response ->
                response.updatedProducts?.map { productDtoMapper.mapToModel(site.localId(), it) }
            }
        }

    suspend fun replyToReview(
        site: SiteModel,
        productId: RemoteId,
        reviewId: RemoteId,
        replyContent: String?
    ): WooPayload<Unit> {
        val body = mapOf(
            "post" to productId.value,
            "parent" to reviewId.value,
            "content" to replyContent.orEmpty()
        )

        return wooNetwork.executePostGsonRequest(
            site = site,
            path = WPAPI.comments.urlV2,
            clazz = Unit::class.java,
            body = body
        ).toWooPayload()
    }

    /**
     * Makes a POST request to `/wp-json/wc/v3/products/[WCProductModel.remoteProductId]/variations/batch`
     * This API helps you to batch create, update and delete multiple product variations.
     *
     * @param [site] The site containing the product
     * @param productId Id of the product.
     * @param createVariations list of product variations to create.
     * The Map keys correspond to the names of variation properties.
     * The Map values are the variation properties values.
     * @param updateVariations list of product variations to update.
     * The Map keys correspond to the names of variation properties.
     * The Map values are the variation properties values.
     * @param deleteVariations list of product variations ids to delete.
     *
     * @return Instance of [BatchProductVariationsApiResponse].
     */
    suspend fun batchUpdateVariations(
        site: SiteModel,
        productId: Long,
        createVariations: List<Map<String, Any>>? = null,
        updateVariations: List<Map<String, Any>>? = null,
        deleteVariations: List<Long>? = null
    ): WooPayload<BatchProductVariationsApiResponse> =
        WOOCOMMERCE.products.id(productId).variations.batch.pathV3
            .let { url ->
                val body = buildMap {
                    putIfNotNull("create" to createVariations)
                    putIfNotNull("update" to updateVariations)
                    putIfNotNull("delete" to deleteVariations)
                }

                require(body.isNotEmpty())

                wooNetwork.executePostGsonRequest(
                    site = site,
                    path = url,
                    clazz = BatchProductVariationsApiResponse::class.java,
                    body = body
                ).toWooPayload()
            }

    suspend fun createVariations(
        site: SiteModel,
        variations: List<Map<String, Any>>,
        productId: RemoteId
    ): WooPayload<BatchProductVariationsApiResponse> =
        WOOCOMMERCE.products.id(productId.value).variations.batch.pathV3.let { url ->
            val body = buildMap {
                putIfNotNull("create" to variations)
            }

            wooNetwork.executePostGsonRequest(
                site = site,
                path = url,
                clazz = BatchProductVariationsApiResponse::class.java,
                body = body
            ).toWooPayload()
        }

    /**
     * Makes a POST request to `/wp-json/wc/v3/products` to create
     * a empty variation to a given variable product
     *
     * @param [site] The site containing the product
     * @param [productId] the ID of the variable product to create the empty variation
     */
    suspend fun generateEmptyVariation(
        site: SiteModel,
        productId: Long,
        attributesJson: String
    ) = WOOCOMMERCE.products.id(productId).variations.pathV3
        .let { url ->
            wooNetwork.executePostGsonRequest(
                site = site,
                path = url,
                clazz = ProductVariationApiResponse::class.java,
                body = mapOf("attributes" to JsonParser().parse(attributesJson).asJsonArray)
            ).toWooPayload()
        }

    /**
     * Makes a DELETE request to `/wp-json/wc/v3/products/<id>` to delete a product
     *
     * @param [site] The site containing the product
     * @param [productId] the ID of the variable product who holds the variation to be deleted
     * @param [variationId] the ID of the variation model to delete
     *
     * Force delete option is not available as Variation doesn't support trashing
     */
    suspend fun deleteVariation(
        site: SiteModel,
        productId: Long,
        variationId: Long
    ) = WOOCOMMERCE.products.id(productId).variations.variation(variationId).pathV3
        .let { url ->
            wooNetwork.executeDeleteGsonRequest(
                site = site,
                path = url,
                clazz = ProductVariationApiResponse::class.java
            ).toWooPayload()
        }

    /**
     * Makes a PUT request to
     * `/wp-json/wc/v3/products/[WCProductModel.remoteProductId]/variations/[WCProductVariationModel.remoteVariationId]`
     * to replace a variation's attributes with [WCProductVariationModel.attributes]
     *
     * Returns a WooPayload with the Api response as result
     *
     * @param [site] The site to update the given variation attributes
     * @param [attributesJson] Locally updated product variation to be sent
     */

    suspend fun updateVariationAttributes(
        site: SiteModel,
        productId: Long,
        variationId: Long,
        attributesJson: String
    ) = WOOCOMMERCE.products.id(productId).variations.variation(variationId).pathV3
        .let { url ->
            wooNetwork.executePutGsonRequest(
                site = site,
                path = url,
                clazz = ProductVariationApiResponse::class.java,
                body = mapOf("attributes" to JsonParser().parse(attributesJson).asJsonArray)
            ).toWooPayload()
        }

    /**
     * Makes a PUT request to `/wp-json/wc/v3/products/[WCProductModel.remoteProductId]`
     * to replace a product's attributes with [WCProductModel.attributes]
     *
     * Returns a WooPayload with the Api response as result
     *
     * @param [site] The site to update the given product attributes
     */

    suspend fun updateProductAttributes(
        site: SiteModel,
        productId: Long,
        attributesJson: String
    ): WooPayload<ProductWithMetaData> = WOOCOMMERCE.products.id(productId).pathV3
        .let { url ->
            wooNetwork.executePutGsonRequest(
                site = site,
                path = url,
                clazz = ProductApiResponse::class.java,
                body = mapOf("attributes" to JsonParser().parse(attributesJson).asJsonArray)
            ).toWooPayload { dto ->
                productDtoMapper.mapToModel(site.localId(), dto)
            }
        }

    /**
     * Makes a PUT request to `/wp-json/wc/v3/products/[remoteProductId]` to replace a product's images
     * with the passed media list
     *
     * Dispatches a WCProductAction.UPDATED_PRODUCT_IMAGES action with the result
     *
     * @param [site] The site to update product images for
     * @param [remoteProductId] Unique server id of the product to update
     * @param [imageList] list of product images to assign to the product
     */
    fun updateProductImages(
        site: SiteModel,
        remoteProductId: Long,
        imageList: List<WCProductImageModel>
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "updateProductImages") {
            val url = WOOCOMMERCE.products.id(remoteProductId).pathV3

            // build json list of images
            val jsonBody = JsonArray()
            for (image in imageList) {
                jsonBody.add(image.toJson())
            }
            val body = HashMap<String, Any>()
            body["id"] = remoteProductId
            body["images"] = jsonBody

            val response = wooNetwork.executePutGsonRequest(
                site = site,
                path = url,
                body = body,
                clazz = ProductApiResponse::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    response.data?.let {
                        val newModel = productDtoMapper.mapToModel(site.localId(), it)
                        val payload = RemoteUpdateProductImagesPayload(site, newModel)
                        dispatcher.dispatch(
                            WCProductActionBuilder.newUpdatedProductImagesAction(
                                payload
                            )
                        )
                    }
                }

                is WPAPIResponse.Error -> {
                    val productError = wpAPINetworkErrorToProductError(response.error)
                    val payload = RemoteUpdateProductImagesPayload(
                        productError,
                        site,
                        ProductWithMetaData(WCProductModel().apply { this.remoteProductId = remoteProductId })
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newUpdatedProductImagesAction(payload))
                }
            }
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products/categories` retrieving a list of product
     * categories for a given WooCommerce [SiteModel].
     *
     * The number of categories to fetch is defined in [WCProductStore.DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE],
     * and retrieving older categories is done by passing an [offset].
     *
     * Dispatches a [WCProductAction.FETCHED_PRODUCT_CATEGORIES]
     *
     * @param [site] The site to fetch product categories for
     * @param [offset] The offset to use for the fetch
     * @param [productCategorySorting] Optional. The sorting type of the categories
     */
    fun fetchProductCategories(
        site: SiteModel,
        pageSize: Int = DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE,
        offset: Int = 0,
        productCategorySorting: ProductCategorySorting? = DEFAULT_CATEGORY_SORTING
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "fetchProductCategories") {
            val sortOrder = when (productCategorySorting) {
                NAME_DESC -> "desc"
                else -> "asc"
            }

            val url = WOOCOMMERCE.products.categories.pathV3
            val params = mutableMapOf(
                "per_page" to pageSize.toString(),
                "offset" to offset.toString(),
                "order" to sortOrder,
                "orderby" to "name"
            )

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                params = params,
                clazz = Array<ProductCategoryApiResponse>::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    response.data?.let {
                        val categories = it.map { category ->
                            category.asProductCategoryModel().apply { localSiteId = site.id }
                        }
                        val canLoadMore = categories.size == pageSize
                        val loadedMore = offset > 0
                        val payload = RemoteProductCategoriesPayload(
                            site, categories, offset, loadedMore, canLoadMore
                        )
                        dispatcher.dispatch(
                            WCProductActionBuilder.newFetchedProductCategoriesAction(
                                payload
                            )
                        )
                    }
                }

                is WPAPIResponse.Error -> {
                    val productCategoryError = wpAPINetworkErrorToProductError(response.error)
                    val payload = RemoteProductCategoriesPayload(productCategoryError, site)
                    dispatcher.dispatch(
                        WCProductActionBuilder.newFetchedProductCategoriesAction(
                            payload
                        )
                    )
                }
            }
        }
    }

    suspend fun addProductCategories(
        site: SiteModel,
        categories: List<WCProductCategoryModel>
    ): WooPayload<List<WCProductCategoryModel>> {
        val path = WOOCOMMERCE.products.categories.batch.pathV3

        val body = mutableMapOf(
            "create" to categories.map { category ->
                mapOf(
                    "name" to category.name,
                    "parent" to category.parent.toString()
                )
            }
        )

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = path,
            body = body,
            clazz = ProductCategoryBatchApiResponse::class.java
        )

        return when {
            response is WPAPIResponse.Success && response.data == null -> WooPayload(
                WooError(
                    type = WooErrorType.EMPTY_RESPONSE,
                    original = GenericErrorType.UNKNOWN,
                    message = "Success response with empty data"
                )
            )

            response is WPAPIResponse.Success -> {
                WooPayload(
                    response.data!!.createdCategories
                        .filter { it.error == null }
                        .map {
                            it.asProductCategoryModel().apply {
                                localSiteId = site.id
                            }
                        }
                )
            }

            else -> return WooPayload(
                error = (response as WPAPIResponse.Error).error.toWooError()
            )
        }
    }

    /**
     * Posts a new Add Category record to the API for a category.
     *
     * Makes a POST request to `/wp-json/wc/v3/products/categories/id` to save a Category record.
     * Returns a [WCProductCategoryModel] on successful response.
     */
    suspend fun addProductCategory(
        site: SiteModel,
        category: WCProductCategoryModel
    ): WooPayload<WCProductCategoryModel> {
        val url = WOOCOMMERCE.products.categories.pathV3

        val body = mutableMapOf(
            "name" to category.name,
            "parent" to category.parent
        )

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            body = body,
            clazz = ProductCategoryApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                val updatedCategory = response.data?.let {
                    it.asProductCategoryModel().apply {
                        localSiteId = site.id
                    }
                }
                WooPayload(updatedCategory)
            }

            is WPAPIResponse.Error -> {
                val productCategorySaveError = wpAPINetworkErrorToProductError(response.error)
                when (productCategorySaveError.type) {
                    TERM_EXISTS -> {
                        WooPayload(
                            error = WooError(
                                type = WooErrorType.RESOURCE_ALREADY_EXISTS,
                                original = GenericErrorType.UNKNOWN,
                                message = "Product category already exists."
                            )
                        )
                    }

                    else -> {
                        WooPayload(
                            error = WooError(
                                type = WooErrorType.GENERIC_ERROR,
                                original = GenericErrorType.UNKNOWN,
                                message = "Unknown server error while adding a new product category."
                            )
                        )
                    }
                }
            }

            else -> {
                WooPayload(
                    error = WooError(
                        type = WooErrorType.EMPTY_RESPONSE,
                        original = GenericErrorType.UNKNOWN,
                        message = "Invalid response for adding a new product category."
                    )
                )
            }
        }
    }

    suspend fun updateProductCategory(
        site: SiteModel,
        category: WCProductCategoryModel
    ): WooPayload<WCProductCategoryModel> {
        val path = WOOCOMMERCE.products.categories.id(category.remoteCategoryId).pathV3

        val body = mutableMapOf(
            "name" to category.name,
            "parent" to category.parent
        )

        val response = wooNetwork.executePutGsonRequest(
            site = site,
            path = path,
            body = body,
            clazz = ProductCategoryApiResponse::class.java
        )

        return when {
            response is WPAPIResponse.Success -> {
                val updatedCategory = response.data?.let {
                    it.asProductCategoryModel().apply {
                        localSiteId = site.id
                    }
                }
                WooPayload(updatedCategory)
            }

            else -> return WooPayload(
                error = (response as WPAPIResponse.Error).error.toWooError()
            )
        }
    }

    suspend fun deleteProductCategory(site: SiteModel, remoteId: Long): WooPayload<WCProductCategoryModel> {
        val path = WOOCOMMERCE.products.categories.id(remoteId).pathV3

        val params = mutableMapOf(
            "force" to "true",
        )
        val response = wooNetwork.executeDeleteGsonRequest(
            site = site,
            path = path,
            params = params,
            clazz = ProductCategoryApiResponse::class.java
        )

        return when {
            response is WPAPIResponse.Success -> {
                val updatedCategory = response.data?.let {
                    it.asProductCategoryModel().apply {
                        localSiteId = site.id
                    }
                }
                WooPayload(updatedCategory)
            }

            else -> return WooPayload(
                error = (response as WPAPIResponse.Error).error.toWooError()
            )
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products/reviews` retrieving a list of product reviews
     * for a given WooCommerce [SiteModel].
     *
     * The number of reviews to fetch is defined in [WCProductStore.NUM_REVIEWS_PER_FETCH], and retrieving older
     * reviews is done by passing an [offset].
     *
     * @param [site] The site to fetch product reviews for
     * @param [offset] The offset to use for the fetch
     * @param [reviewIds] Optional. A list of remote product review ID's to fetch
     * @param [productIds] Optional. A list of remote product ID's to fetch product reviews for
     * @param [filterByStatus] Optional. A list of product review statuses to fetch
     */
    suspend fun fetchProductReviews(
        site: SiteModel,
        offset: Int,
        reviewIds: List<Long>? = null,
        productIds: List<Long>? = null,
        filterByStatus: List<String>? = null
    ): FetchProductReviewsResponsePayload {
        val statusFilter = filterByStatus?.joinToString { it } ?: "all"

        val url = WOOCOMMERCE.products.reviews.pathV3
        val params = mutableMapOf(
            "per_page" to WCProductStore.NUM_REVIEWS_PER_FETCH.toString(),
            "offset" to offset.toString(),
            "status" to statusFilter
        )
        reviewIds?.let { ids ->
            params.put("include", ids.map { it }.joinToString())
        }
        productIds?.let { ids ->
            params.put("product", ids.map { it }.joinToString())
        }

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<ProductReviewApiResponse>::class.java,
            params = params
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                val productData = response.data
                if (productData != null) {
                    val reviews = productData.map { review ->
                        productReviewResponseToProductReviewModel(review).apply { localSiteId = site.id }
                    }
                    FetchProductReviewsResponsePayload(
                        site,
                        reviews,
                        productIds,
                        filterByStatus,
                        canLoadMore = reviews.size == WCProductStore.NUM_REVIEWS_PER_FETCH
                    )
                } else {
                    FetchProductReviewsResponsePayload(
                        ProductError(
                            GENERIC_ERROR,
                            "Success response with empty data"
                        ),
                        site
                    )
                }
            }

            is WPAPIResponse.Error -> {
                FetchProductReviewsResponsePayload(
                    wpAPINetworkErrorToProductError(response.error),
                    site
                )
            }
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/products/reviews/<id>` retrieving a product review by
     * its remote ID for a given WooCommerce [SiteModel].
     *
     * @param [site] The site to fetch product reviews for
     * @param [remoteReviewId] The remote id of the review to fetch
     */
    suspend fun fetchProductReviewById(
        site: SiteModel,
        remoteReviewId: Long
    ): RemoteProductReviewPayload {
        val url = WOOCOMMERCE.products.reviews.id(remoteReviewId).pathV3
        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = ProductReviewApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    val review = productReviewResponseToProductReviewModel(it).apply {
                        localSiteId = site.id
                    }
                    RemoteProductReviewPayload(site, review)
                } ?: RemoteProductReviewPayload(
                    error = ProductError(GENERIC_ERROR, "Success response with empty data"),
                    site = site
                )
            }

            is WPAPIResponse.Error -> {
                val productReviewError = wpAPINetworkErrorToProductError(response.error)
                RemoteProductReviewPayload(error = productReviewError, site = site)
            }
        }
    }

    /**
     * Makes a PUT request to `/wp-json/wc/v3/products/reviews/<id>` updating the status for the
     * given product review to [newStatus].
     *
     * @param [site] The site to fetch product reviews for
     * @param [remoteReviewId] The remote ID of the product review to be updated
     * @param [newStatus] The new status to update the product review to
     *
     * @return [WooPayload] with the updated [WCProductReviewModel]
     */
    suspend fun updateProductReviewStatus(
        site: SiteModel,
        remoteReviewId: Long,
        newStatus: String
    ): WooPayload<WCProductReviewModel> {
        val url = WOOCOMMERCE.products.reviews.id(remoteReviewId).pathV3
        val params = mapOf("status" to newStatus)
        val response = wooNetwork.executePutGsonRequest(
            site = site,
            path = url,
            clazz = ProductReviewApiResponse::class.java,
            body = params
        )

        return response.toWooPayload {
            productReviewResponseToProductReviewModel(it).apply {
                localSiteId = site.id
            }
        }
    }

    /**
     * Makes a POST request to `/wp-json/wc/v3/products` to add a product
     *
     * Dispatches a [WCProductAction.ADDED_PRODUCT] action with the result
     *
     * @param [site] The site to fetch product reviews for
     * @param [productModel] the new product model
     * @param [metaData] the metadata to be added to the product
     */
    fun addProduct(
        site: SiteModel,
        productModel: WCProductModel,
        metaData: Map<String, WCMetaDataValue>? = null
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "addProduct") {
            val url = WOOCOMMERCE.products.pathV3
            val body = productModelToProductJsonBody(
                productModel = null,
                updatedProductModel = productModel,
                metadata = metaData?.let {
                    JsonArray().apply {
                        it.forEach { (key, value) ->
                            add(JsonObject().apply {
                                addProperty(WCMetaData.KEY, key)
                                add(WCMetaData.VALUE, value.jsonValue)
                            })
                        }
                    }
                }
            )

            val response = wooNetwork.executePostGsonRequest(
                site = site,
                path = url,
                body = body,
                clazz = ProductApiResponse::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    response.data?.let { dto ->
                        val newModel = productDtoMapper.mapToModel(site.localId(), dto)
                        val payload = RemoteAddProductPayload(site, newModel)
                        dispatcher.dispatch(WCProductActionBuilder.newAddedProductAction(payload))
                    }
                }

                is WPAPIResponse.Error -> {
                    val productError = wpAPINetworkErrorToProductError(response.error)
                    val payload = RemoteAddProductPayload(
                        productError,
                        site,
                        ProductWithMetaData(WCProductModel())
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newAddedProductAction(payload))
                }
            }
        }
    }

    /**
     * Makes a DELETE request to `/wp-json/wc/v3/products/<id>` to delete a product
     *
     * Dispatches a [WCProductAction.DELETED_PRODUCT] action with the result
     *
     * @param [site] The site containing the product
     * @param [remoteProductId] the ID of the product model to delete
     * @param [forceDelete] whether to permanently delete the product (will be sent to trash if false)
     */
    fun deleteProduct(
        site: SiteModel,
        remoteProductId: Long,
        forceDelete: Boolean = false
    ) {
        coroutineEngine.launch(AppLog.T.API, this, "deleteProduct") {
            val url = WOOCOMMERCE.products.id(remoteProductId).pathV3
            val params = mapOf("force" to forceDelete.toString())

            val response = wooNetwork.executeDeleteGsonRequest(
                site = site,
                path = url,
                params = params,
                clazz = ProductApiResponse::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    response.data?.let {
                        val payload = RemoteDeleteProductPayload(site, remoteProductId)
                        dispatcher.dispatch(WCProductActionBuilder.newDeletedProductAction(payload))
                    }
                }

                is WPAPIResponse.Error -> {
                    val productError = wpAPINetworkErrorToProductError(response.error)
                    val payload = RemoteDeleteProductPayload(
                        productError,
                        site,
                        remoteProductId
                    )
                    dispatcher.dispatch(WCProductActionBuilder.newDeletedProductAction(payload))
                }
            }
        }
    }

    /**
     * Build json body of product items to be updated to the backend.
     *
     * This method checks if there is a cached version of the product stored locally.
     * If not, it generates a new product model for the same product ID, with default fields
     * and verifies that the [updatedProductModel] has fields that are different from the default
     * fields of [productModel]. This is to ensure that we do not update product fields that do not contain any changes
     */
    @Suppress("LongMethod", "ComplexMethod", "SwallowedException", "TooGenericExceptionCaught")
    private fun productModelToProductJsonBody(
        productModel: WCProductModel?,
        updatedProductModel: WCProductModel,
        metadata: JsonArray? = null
    ): HashMap<String, Any> {
        val body = HashMap<String, Any>()

        val storedWCProductModel = productModel ?: WCProductModel().apply {
            remoteProductId = updatedProductModel.remoteProductId
        }
        if (storedWCProductModel.description != updatedProductModel.description) {
            body["description"] = updatedProductModel.description
        }
        if (storedWCProductModel.name != updatedProductModel.name) {
            body["name"] = updatedProductModel.name
        }
        if (storedWCProductModel.type != updatedProductModel.type) {
            body["type"] = updatedProductModel.type
        }
        if (storedWCProductModel.sku != updatedProductModel.sku) {
            body["sku"] = updatedProductModel.sku
        }
        if (storedWCProductModel.globalUniqueId != updatedProductModel.globalUniqueId) {
            body["global_unique_id"] = updatedProductModel.globalUniqueId
        }
        if (storedWCProductModel.status != updatedProductModel.status) {
            body["status"] = updatedProductModel.status
        }
        if (storedWCProductModel.catalogVisibility != updatedProductModel.catalogVisibility) {
            body["catalog_visibility"] = updatedProductModel.catalogVisibility
        }
        if (storedWCProductModel.slug != updatedProductModel.slug) {
            body["slug"] = updatedProductModel.slug
        }
        if (storedWCProductModel.featured != updatedProductModel.featured) {
            body["featured"] = updatedProductModel.featured
        }
        if (storedWCProductModel.manageStock != updatedProductModel.manageStock) {
            body["manage_stock"] = updatedProductModel.manageStock
        }
        if (storedWCProductModel.externalUrl != updatedProductModel.externalUrl) {
            body["external_url"] = updatedProductModel.externalUrl
        }
        if (storedWCProductModel.buttonText != updatedProductModel.buttonText) {
            body["button_text"] = updatedProductModel.buttonText
        }

        // only allowed to change the following params if manageStock is enabled
        if (updatedProductModel.manageStock) {
            if (storedWCProductModel.stockQuantity != updatedProductModel.stockQuantity) {
                // Conversion/rounding down because core API only accepts Int value for stock quantity.
                // On the app side, make sure it only allows whole decimal quantity when updating, so that
                // there's no undesirable conversion effect.
                body["stock_quantity"] = updatedProductModel.stockQuantity.toInt()
            }
            if (storedWCProductModel.backorders != updatedProductModel.backorders) {
                body["backorders"] = updatedProductModel.backorders
            }
        }
        if (storedWCProductModel.stockStatus != updatedProductModel.stockStatus) {
            body["stock_status"] = updatedProductModel.stockStatus
        }
        if (storedWCProductModel.soldIndividually != updatedProductModel.soldIndividually) {
            body["sold_individually"] = updatedProductModel.soldIndividually
        }
        if (storedWCProductModel.regularPrice != updatedProductModel.regularPrice) {
            body["regular_price"] = updatedProductModel.regularPrice
        }
        if (storedWCProductModel.salePrice != updatedProductModel.salePrice) {
            body["sale_price"] = updatedProductModel.salePrice
        }
        if (storedWCProductModel.dateOnSaleFromGmt != updatedProductModel.dateOnSaleFromGmt) {
            body["date_on_sale_from_gmt"] = updatedProductModel.dateOnSaleFromGmt
        }
        if (storedWCProductModel.dateOnSaleToGmt != updatedProductModel.dateOnSaleToGmt) {
            body["date_on_sale_to_gmt"] = updatedProductModel.dateOnSaleToGmt
        }
        if (storedWCProductModel.taxStatus != updatedProductModel.taxStatus) {
            body["tax_status"] = updatedProductModel.taxStatus
        }
        if (storedWCProductModel.taxClass != updatedProductModel.taxClass) {
            body["tax_class"] = updatedProductModel.taxClass
        }
        if (storedWCProductModel.weight != updatedProductModel.weight) {
            body["weight"] = updatedProductModel.weight
        }

        val dimensionsBody = mutableMapOf<String, String>()
        if (storedWCProductModel.height != updatedProductModel.height) {
            dimensionsBody["height"] = updatedProductModel.height
        }
        if (storedWCProductModel.width != updatedProductModel.width) {
            dimensionsBody["width"] = updatedProductModel.width
        }
        if (storedWCProductModel.length != updatedProductModel.length) {
            dimensionsBody["length"] = updatedProductModel.length
        }
        if (dimensionsBody.isNotEmpty()) {
            body["dimensions"] = dimensionsBody
        }
        if (storedWCProductModel.shippingClass != updatedProductModel.shippingClass) {
            body["shipping_class"] = updatedProductModel.shippingClass
        }
        if (storedWCProductModel.shortDescription != updatedProductModel.shortDescription) {
            body["short_description"] = updatedProductModel.shortDescription
        }
        if (!storedWCProductModel.hasSameImages(updatedProductModel)) {
            val updatedImages = updatedProductModel.getImageListOrEmpty()
            body["images"] = JsonArray().also {
                for (image in updatedImages) {
                    it.add(image.toJson())
                }
            }
        }
        if (storedWCProductModel.reviewsAllowed != updatedProductModel.reviewsAllowed) {
            body["reviews_allowed"] = updatedProductModel.reviewsAllowed
        }
        if (storedWCProductModel.virtual != updatedProductModel.virtual) {
            body["virtual"] = updatedProductModel.virtual
        }
        if (storedWCProductModel.purchaseNote != updatedProductModel.purchaseNote) {
            body["purchase_note"] = updatedProductModel.purchaseNote
        }
        if (storedWCProductModel.menuOrder != updatedProductModel.menuOrder) {
            body["menu_order"] = updatedProductModel.menuOrder
        }
        if (!storedWCProductModel.hasSameCategories(updatedProductModel)) {
            val updatedCategories = updatedProductModel.getCategoryList()
            body["categories"] = JsonArray().also {
                for (category in updatedCategories) {
                    it.add(category.toJson())
                }
            }
        }
        if (!storedWCProductModel.hasSameTags(updatedProductModel)) {
            val updatedTags = updatedProductModel.getTagList()
            body["tags"] = JsonArray().also {
                for (tag in updatedTags) {
                    it.add(tag.toJson())
                }
            }
        }
        if (storedWCProductModel.groupedProductIds != updatedProductModel.groupedProductIds) {
            body["grouped_products"] = updatedProductModel.getGroupedProductIdList()
        }
        if (storedWCProductModel.crossSellIds != updatedProductModel.crossSellIds) {
            body["cross_sell_ids"] = updatedProductModel.getCrossSellProductIdList()
        }
        if (storedWCProductModel.upsellIds != updatedProductModel.upsellIds) {
            body["upsell_ids"] = updatedProductModel.getUpsellProductIdList()
        }
        if (storedWCProductModel.downloadable != updatedProductModel.downloadable) {
            body["downloadable"] = updatedProductModel.downloadable
        }
        if (storedWCProductModel.downloadLimit != updatedProductModel.downloadLimit) {
            body["download_limit"] = updatedProductModel.downloadLimit
        }
        if (storedWCProductModel.downloadExpiry != updatedProductModel.downloadExpiry) {
            body["download_expiry"] = updatedProductModel.downloadExpiry
        }

        if (storedWCProductModel.minAllowedQuantity != updatedProductModel.minAllowedQuantity) {
            body["min_quantity"] = updatedProductModel.minAllowedQuantity
        }

        if (storedWCProductModel.maxAllowedQuantity != updatedProductModel.maxAllowedQuantity) {
            body["max_quantity"] = updatedProductModel.maxAllowedQuantity
        }

        if (storedWCProductModel.groupOfQuantity != updatedProductModel.groupOfQuantity) {
            body["group_of_quantity"] = updatedProductModel.groupOfQuantity
        }

        if (!storedWCProductModel.hasSameDownloadableFiles(updatedProductModel)) {
            val updatedFiles = updatedProductModel.getDownloadableFiles()
            body["downloads"] = JsonArray().apply {
                updatedFiles.forEach { file ->
                    add(file.toJson())
                }
            }
        }
        if (!storedWCProductModel.hasSameAttributes(updatedProductModel)) {
            JsonParser().apply {
                body["attributes"] = try {
                    parse(updatedProductModel.attributes).asJsonArray
                } catch (ex: Exception) {
                    JsonArray()
                }
            }
        }
        if (metadata != null) {
            body["meta_data"] = metadata
        }
        if (storedWCProductModel.password != updatedProductModel.password) {
            body["post_password"] = updatedProductModel.password.orEmpty()
        }

        return body
    }

    private fun productTagApiResponseToProductTagModel(
        response: ProductTagApiResponse,
        site: SiteModel
    ): WCProductTagModel {
        return WCProductTagModel().apply {
            remoteTagId = response.id
            localSiteId = site.id
            name = response.name ?: ""
            slug = response.slug ?: ""
            description = response.description ?: ""
            count = response.count
        }
    }

    private fun productShippingClassResponseToProductShippingClassModel(
        response: ProductShippingClassApiResponse,
        site: SiteModel
    ): WCProductShippingClassModel {
        return WCProductShippingClassModel().apply {
            remoteShippingClassId = response.id
            localSiteId = site.id
            name = response.name ?: ""
            slug = response.slug ?: ""
            description = response.description ?: ""
        }
    }

    private fun productReviewResponseToProductReviewModel(response: ProductReviewApiResponse): WCProductReviewModel {
        return WCProductReviewModel().apply {
            remoteProductReviewId = response.id
            remoteProductId = response.product_id
            dateCreated = response.date_created_gmt?.let { "${it}Z" } ?: ""
            status = response.status ?: ""
            reviewerName = response.reviewer ?: ""
            reviewerEmail = response.reviewer_email ?: ""
            review = response.review ?: ""
            rating = response.rating
            verified = response.verified
            reviewerAvatarsJson = response.reviewer_avatar_urls?.toString() ?: ""
        }
    }

    private fun networkErrorToProductError(wpComError: WPComGsonNetworkError): ProductError {
        val productErrorType = when {
            wpComError.apiError == "woocommerce_rest_product_invalid_id" ->
                ProductErrorType.INVALID_PRODUCT_ID

            wpComError.apiError == "rest_invalid_param" -> ProductErrorType.INVALID_PARAM
            wpComError.apiError == "woocommerce_rest_review_invalid_id" ->
                ProductErrorType.INVALID_REVIEW_ID

            wpComError.apiError == "woocommerce_product_invalid_image_id" ->
                ProductErrorType.INVALID_IMAGE_ID

            wpComError.apiError == "product_invalid_sku" -> ProductErrorType.DUPLICATE_SKU
            wpComError.apiError == "term_exists" -> ProductErrorType.TERM_EXISTS
            wpComError.apiError == "woocommerce_variation_invalid_image_id" ->
                ProductErrorType.INVALID_VARIATION_IMAGE_ID

            wpComError.type == PARSE_ERROR -> ProductErrorType.PARSE_ERROR
            else -> ProductErrorType.fromString(wpComError.apiError)
        }
        return ProductError(productErrorType, wpComError.combinedErrorMessage)
    }

    private fun wpAPINetworkErrorToProductError(wpAPINetworkError: WPAPINetworkError): ProductError {
        val productErrorType = when {
            wpAPINetworkError.errorCode == "woocommerce_rest_product_invalid_id" ->
                ProductErrorType.INVALID_PRODUCT_ID

            wpAPINetworkError.errorCode == "rest_invalid_param" -> ProductErrorType.INVALID_PARAM
            wpAPINetworkError.errorCode == "woocommerce_rest_review_invalid_id" ->
                ProductErrorType.INVALID_REVIEW_ID

            wpAPINetworkError.errorCode == "woocommerce_product_invalid_image_id" ->
                ProductErrorType.INVALID_IMAGE_ID

            wpAPINetworkError.errorCode == "product_invalid_sku" -> ProductErrorType.DUPLICATE_SKU
            wpAPINetworkError.errorCode == "term_exists" -> ProductErrorType.TERM_EXISTS
            wpAPINetworkError.errorCode == "woocommerce_variation_invalid_image_id" ->
                ProductErrorType.INVALID_VARIATION_IMAGE_ID

            wpAPINetworkError.errorCode == "woocommerce_rest_invalid_min_quantity" ||
                wpAPINetworkError.errorCode == "woocommerce_rest_invalid_max_quantity" ||
                wpAPINetworkError.errorCode == "woocommerce_rest_invalid_variation_min_quantity" ||
                wpAPINetworkError.errorCode == "woocommerce_rest_invalid_variation_max_quantity" ->
                ProductErrorType.INVALID_MIN_MAX_QUANTITY

            wpAPINetworkError.type == PARSE_ERROR -> ProductErrorType.PARSE_ERROR
            else -> ProductErrorType.fromString(wpAPINetworkError.errorCode.orEmpty())
        }
        return ProductError(productErrorType, wpAPINetworkError.combinedErrorMessage)
    }
}
