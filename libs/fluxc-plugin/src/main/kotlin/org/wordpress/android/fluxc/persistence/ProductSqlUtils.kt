package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.wellsql.generated.WCProductCategoryModelTable
import com.wellsql.generated.WCProductModelTable
import com.wellsql.generated.WCProductReviewModelTable
import com.wellsql.generated.WCProductShippingClassModelTable
import com.wellsql.generated.WCProductTagModelTable
import com.wellsql.generated.WCProductVariationModelTable
import com.yarolegovich.wellsql.ConditionClauseBuilder
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCBundledProduct
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductComponent
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_CATEGORY_SORTING
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_PRODUCT_SORTING
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting.NAME_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting.NAME_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.DATE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_DESC
import org.wordpress.android.fluxc.store.WCProductStore.SkuSearchOptions
import java.util.Locale

@Suppress("LargeClass")
object ProductSqlUtils {
    private const val DEBOUNCE_DELAY_FOR_OBSERVERS = 50L
    private val productsUpdatesTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val variationsUpdatesTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val categoriesUpdatesTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val gson by lazy { Gson() }

    fun observeProductsCount(
        site: SiteModel,
        filterOptions: Map<ProductFilterOption, String> = emptyMap(),
        excludeSampleProducts: Boolean = false
    ): Flow<Long> {
        return productsUpdatesTrigger
            .onStart { emit(Unit) }
            .debounce(DEBOUNCE_DELAY_FOR_OBSERVERS)
            .mapLatest {
                getProductCountForSite(site, filterOptions, excludeSampleProducts)
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    fun observeProducts(
        site: SiteModel,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        filterOptions: Map<ProductFilterOption, String> = emptyMap(),
        excludeSampleProducts: Boolean = false,
        limit: Int? = null
    ): Flow<List<WCProductModel>> {
        return productsUpdatesTrigger
            .onStart { emit(Unit) }
            .debounce(DEBOUNCE_DELAY_FOR_OBSERVERS)
            .mapLatest {
                if (filterOptions.isEmpty()) {
                    getProductsForSite(
                        site = site,
                        sortType = sortType,
                        excludeSampleProducts = excludeSampleProducts,
                        limit = limit
                    )
                } else {
                    getProducts(
                        site = site,
                        filterOptions = filterOptions,
                        sortType = sortType,
                        excludeSampleProducts = excludeSampleProducts,
                        limit = limit
                    )
                }
            }
            .flowOn(Dispatchers.IO)
    }

    fun observeVariations(site: SiteModel, productId: Long): Flow<List<WCProductVariationModel>> {
        return variationsUpdatesTrigger
            .onStart { emit(Unit) }
            .debounce(DEBOUNCE_DELAY_FOR_OBSERVERS)
            .mapLatest {
                getVariationsForProduct(site, productId)
            }
            .flowOn(Dispatchers.IO)
    }

    fun observeCategories(site: SiteModel, sortType: ProductCategorySorting): Flow<List<WCProductCategoryModel>> {
        return categoriesUpdatesTrigger
            .onStart { emit(Unit) }
            .debounce(DEBOUNCE_DELAY_FOR_OBSERVERS)
            .mapLatest {
                getProductCategoriesForSite(site, sortType)
            }
            .flowOn(Dispatchers.IO)
    }

    fun getCompositeProducts(site: SiteModel, remoteProductId: Long): List<WCProductComponent> {
        val productModel = getProductByRemoteId(site, remoteProductId)

        return productModel?.let {
            val responseType = object : TypeToken<List<WCProductComponent>>() {}.type
            gson.fromJson(it.compositeComponents, responseType) as? List<WCProductComponent>
        } ?: emptyList()
    }

    private fun getBundledProducts(site: SiteModel, remoteProductId: Long): List<WCBundledProduct> {
        val productModel = WellSql.select(WCProductModel::class.java)
            .where().beginGroup()
            .equals(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductId)
            .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
            .endGroup().endWhere()
            .asModel.firstOrNull()

        return productModel?.let {
            val responseType = object : TypeToken<List<WCBundledProduct>>() {}.type
            gson.fromJson(it.bundledItems, responseType) as? List<WCBundledProduct>
        } ?: emptyList()
    }

    fun getBundledProductsCount(site: SiteModel, remoteProductId: Long): Int {
        val bundledItems = getBundledProducts(site, remoteProductId)
        return bundledItems.size
    }

    fun observeBundledProducts(
        site: SiteModel,
        remoteProductId: Long
    ): Flow<List<WCBundledProduct>> {
        return productsUpdatesTrigger
            .onStart { emit(Unit) }
            .debounce(DEBOUNCE_DELAY_FOR_OBSERVERS)
            .mapLatest {
                getBundledProducts(site, remoteProductId)
            }
            .flowOn(Dispatchers.IO)
    }

    fun insertOrUpdateProduct(product: WCProductModel): Int {
        val productResult = WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.ID, product.id)
                .or()
                .beginGroup()
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, product.remoteProductId)
                .equals(WCProductModelTable.LOCAL_SITE_ID, product.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (productResult == null) {
            // Insert
            WellSql.insert(product).execute()
            productsUpdatesTrigger.tryEmit(Unit)
            1
        } else {
            // Update
            WellSql.update(WCProductModel::class.java)
                    .where().beginGroup()
                    .equals(WCProductModelTable.REMOTE_PRODUCT_ID, productResult.remoteProductId)
                    .equals(WCProductModelTable.LOCAL_SITE_ID, productResult.localSiteId)
                    .endGroup().endWhere()
                    .put(product, UpdateAllExceptId(WCProductModel::class.java))
                    .execute()
                    .also(::triggerProductsUpdateIfNeeded)
        }
    }

    fun insertOrUpdateProducts(products: List<WCProductModel>): Int {
        var rowsAffected = 0
        executeInTransaction {
            products.forEach {
                rowsAffected += insertOrUpdateProduct(it)
            }
        }
        return rowsAffected
    }

    fun getProductByRemoteId(site: SiteModel, remoteProductId: Long): WCProductModel? {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun getVariationByRemoteId(
        site: SiteModel,
        remoteProductId: Long,
        remoteVariationId: Long
    ): WCProductVariationModel? {
        return WellSql.select(WCProductVariationModel::class.java)
                .where().beginGroup()
                .equals(WCProductVariationModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductVariationModelTable.REMOTE_VARIATION_ID, remoteVariationId)
                .equals(WCProductVariationModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun getProductsByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): List<WCProductModel> {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .isIn(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductIds)
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .asModel
    }

    fun getProductCountByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): Int {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .isIn(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductIds)
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .count().toInt()
    }

    fun getVirtualProductCountByRemoteIds(site: SiteModel, remoteProductIds: List<Long>): Int {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .isIn(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductIds)
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCProductModelTable.VIRTUAL, true)
                .endGroup().endWhere()
                .count().toInt()
    }

    fun getProducts(
        site: SiteModel,
        filterOptions: Map<ProductFilterOption, String>,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        excludedProductIds: List<Long>? = null,
        searchQuery: String? = null,
        skuSearchOptions: SkuSearchOptions = SkuSearchOptions.Disabled,
        excludeSampleProducts: Boolean = false,
        limit: Int? = null
    ): List<WCProductModel> {
        val queryBuilder = WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .applyProductFilterOptions(filterOptions)

        if (searchQuery?.isNotEmpty() == true) {
            when(skuSearchOptions) {
                SkuSearchOptions.Disabled -> {
                    queryBuilder
                        .beginGroup()
                        .contains(WCProductModelTable.NAME, searchQuery)
                        .or()
                        .contains(WCProductModelTable.DESCRIPTION, searchQuery)
                        .or()
                        .contains(WCProductModelTable.SHORT_DESCRIPTION, searchQuery)
                        .endGroup()
                }

                SkuSearchOptions.ExactSearch -> {
                    queryBuilder.beginGroup()
                        // The search is case sensitive
                        .equals(WCProductModelTable.SKU, searchQuery)
                        .endGroup()
                }

                SkuSearchOptions.PartialMatch -> {
                    queryBuilder.beginGroup()
                        .contains(WCProductModelTable.SKU, searchQuery)
                        .endGroup()
                }
            }
        }

        excludedProductIds?.let {
            if (it.isNotEmpty()) {
                queryBuilder.isNotIn(WCProductModelTable.REMOTE_PRODUCT_ID, it)
            }
        }

        if (excludeSampleProducts) {
            queryBuilder.equals(WCProductModelTable.IS_SAMPLE_PRODUCT, false)
        }

        val sortOrder = getSortOrder(sortType)
        val sortField = getSortField(sortType)

        val products = queryBuilder
                .endGroup().endWhere()
                .orderBy(sortField, sortOrder)
                .apply { limit?.let { limit(it) } }
                .asModel

        return if (sortType == TITLE_ASC || sortType == TITLE_DESC) {
            sortProductsByName(products, descending = sortType == TITLE_DESC)
        } else {
            products
        }
    }

    /**
     * WellSQL doesn't support "COLLATE NOCASE" so we have to manually provide case-insensitive sorting
     */
    private fun sortProductsByName(products: List<WCProductModel>, descending: Boolean): List<WCProductModel> {
        return if (descending) {
            products.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
        } else {
            products.sortedBy { it.name.lowercase(Locale.getDefault()) }
        }
    }

    fun geProductExistsByRemoteId(site: SiteModel, remoteProductId: Long): Boolean {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .exists()
    }

    fun getProductExistsBySku(site: SiteModel, sku: String): Boolean {
        return WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.SKU, sku)
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .exists()
    }

    fun getProductsForSite(
        site: SiteModel,
        sortType: ProductSorting = DEFAULT_PRODUCT_SORTING,
        excludeSampleProducts: Boolean = false,
        limit: Int? = null
    ): List<WCProductModel> {
        val sortOrder = getSortOrder(sortType)
        val sortField = getSortField(sortType)
        val products = WellSql.select(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .apply {
                    if (excludeSampleProducts) {
                        equals(WCProductModelTable.IS_SAMPLE_PRODUCT, false)
                    }
                }
                .endGroup().endWhere()
                .orderBy(sortField, sortOrder)
                .apply { limit?.let { limit(it) } }
                .asModel

        return if (sortType == TITLE_ASC || sortType == TITLE_DESC) {
            sortProductsByName(products, descending = sortType == TITLE_DESC)
        } else {
            products
        }
    }

    private fun getSortField(sortType: ProductSorting) =
        when (sortType) {
            TITLE_ASC, TITLE_DESC -> WCProductModelTable.NAME
            DATE_ASC, DATE_DESC -> WCProductModelTable.DATE_CREATED
        }

    private fun getSortOrder(sortType: ProductSorting) =
        when (sortType) {
            TITLE_ASC, DATE_ASC -> SelectQuery.ORDER_ASCENDING
            TITLE_DESC, DATE_DESC -> SelectQuery.ORDER_DESCENDING
        }

    fun deleteProductsForSite(site: SiteModel): Int {
        return WellSql.delete(WCProductModel::class.java)
                .where().beginGroup()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endGroup()
                .endWhere()
                .execute()
                .also(::triggerProductsUpdateIfNeeded)
    }

    fun insertOrUpdateProductVariation(variation: WCProductVariationModel): Int {
        val result = WellSql.select(WCProductVariationModel::class.java)
                .where().beginGroup()
                .equals(WCProductVariationModelTable.ID, variation.id)
                .or()
                .beginGroup()
                .equals(WCProductVariationModelTable.REMOTE_PRODUCT_ID, variation.remoteProductId)
                .equals(WCProductVariationModelTable.REMOTE_VARIATION_ID, variation.remoteVariationId)
                .equals(WCProductVariationModelTable.LOCAL_SITE_ID, variation.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            // Insert
            WellSql.insert(variation).execute()
            variationsUpdatesTrigger.tryEmit(Unit)
            1
        } else {
            // Update
            val oldId = result.id
            WellSql.update(WCProductVariationModel::class.java).whereId(oldId)
                    .put(variation, UpdateAllExceptId(WCProductVariationModel::class.java))
                    .execute()
                    .also(::triggerVariationsUpdateIfNeeded)
        }
    }

    fun insertOrUpdateProductVariations(variations: List<WCProductVariationModel>): Int {
        var rowsAffected = 0
        executeInTransaction {
            variations.forEach {
                rowsAffected += insertOrUpdateProductVariation(it)
            }
        }
        return rowsAffected
    }

    fun getVariationsForProduct(site: SiteModel, remoteProductId: Long): List<WCProductVariationModel> {
        return WellSql.select(WCProductVariationModel::class.java)
                .where()
                .beginGroup()
                .equals(WCProductVariationModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductVariationModelTable.LOCAL_SITE_ID, site.id)
                .endGroup().endWhere()
                .orderBy(WCProductVariationModelTable.MENU_ORDER, SelectQuery.ORDER_ASCENDING)
                .asModel
    }

    fun deleteVariationsForProduct(site: SiteModel, remoteProductId: Long): Int {
        return WellSql.delete(WCProductVariationModel::class.java)
                .where().beginGroup()
                .equals(WCProductVariationModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCProductVariationModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .endGroup()
                .endWhere()
                .execute()
                .also(::triggerVariationsUpdateIfNeeded)
    }

    fun getProductCountForSite(
        site: SiteModel,
        filterOptions: Map<ProductFilterOption, String> = emptyMap(),
        excludeSampleProducts: Boolean = false
    ): Long {
        return WellSql.select(WCProductModel::class.java)
            .where()
            .beginGroup()
            .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
            .applyProductFilterOptions(filterOptions)
            .apply {
                if (excludeSampleProducts) {
                    equals(WCProductModelTable.IS_SAMPLE_PRODUCT, false)
                }
            }
            .endGroup()
            .endWhere()
            .count()
    }

    fun insertOrUpdateProductReviews(productReviews: List<WCProductReviewModel>): Int {
        var rowsAffected = 0
        executeInTransaction {
            productReviews.forEach {
                rowsAffected += insertOrUpdateProductReview(it)
            }
        }
        return rowsAffected
    }

    fun insertOrUpdateProductReview(productReview: WCProductReviewModel): Int {
        val result = WellSql.select(WCProductReviewModel::class.java)
                .where().beginGroup()
                .equals(WCProductReviewModelTable.ID, productReview.id)
                .or()
                .beginGroup()
                .equals(WCProductReviewModelTable.REMOTE_PRODUCT_REVIEW_ID, productReview.remoteProductReviewId)
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, productReview.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            // Insert
            WellSql.insert(productReview).execute()
            1
        } else {
            // Update
            val oldId = result.id
            WellSql.update(WCProductReviewModel::class.java).whereId(oldId)
                    .put(productReview, UpdateAllExceptId(WCProductReviewModel::class.java)).execute()
        }
    }

    fun deleteProductReview(productReview: WCProductReviewModel) =
            WellSql.delete(WCProductReviewModel::class.java)
                    .where()
                    .equals(WCProductReviewModelTable.REMOTE_PRODUCT_REVIEW_ID, productReview.remoteProductReviewId)
                    .endWhere().execute()

    fun getProductReviewByRemoteId(
        localSiteId: Int,
        remoteReviewId: Long
    ): WCProductReviewModel? {
        return WellSql.select(WCProductReviewModel::class.java)
                .where()
                .beginGroup()
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(WCProductReviewModelTable.REMOTE_PRODUCT_REVIEW_ID, remoteReviewId)
                .endGroup()
                .endWhere()
                .asModel.firstOrNull()
    }

    fun getProductReviewsForSite(site: SiteModel): List<WCProductReviewModel> {
        return WellSql.select(WCProductReviewModel::class.java)
                .where()
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .orderBy(WCProductReviewModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .asModel
    }

    fun getProductReviewsByReviewIds(reviewIds: List<Long>): List<WCProductReviewModel> {
        return WellSql.select(WCProductReviewModel::class.java)
                .where()
                .isIn(WCProductReviewModelTable.REMOTE_PRODUCT_REVIEW_ID, reviewIds)
                .endWhere()
                .orderBy(WCProductReviewModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .asModel
    }

    fun getProductReviewsForProductAndSiteId(
        localSiteId: Int,
        remoteProductId: Long
    ): List<WCProductReviewModel> {
        return WellSql.select(WCProductReviewModel::class.java)
                .where().beginGroup()
                .equals(WCProductReviewModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, localSiteId)
                .endGroup().endWhere()
                .orderBy(WCProductReviewModelTable.DATE_CREATED, SelectQuery.ORDER_DESCENDING)
                .asModel
    }

    fun deleteAllProductReviewsForSite(site: SiteModel): Int {
        return WellSql.delete(WCProductReviewModel::class.java)
                .where()
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, site.id)
                .or()
                .equals(WCProductReviewModelTable.LOCAL_SITE_ID, 0) // Should never happen, but sanity cleanup
                .endWhere().execute()
    }

    fun deleteAllProductReviews() = WellSql.delete(WCProductReviewModel::class.java).execute()

    fun updateProductImages(product: WCProductModel, imageList: List<WCProductImageModel>): Int {
        val jsonImageList = JsonArray()
        imageList.forEach { image ->
            JsonObject().also { jsonImage ->
                jsonImage.addProperty("id", image.id)
                jsonImage.addProperty("name", image.name)
                jsonImage.addProperty("src", image.src)
                jsonImage.addProperty("alt", image.alt)
                jsonImageList.add(jsonImage)
            }
        }

        product.images = jsonImageList.toString()
        return insertOrUpdateProduct(product)
    }

    fun deleteProductImage(site: SiteModel, remoteProductId: Long, remoteMediaId: Long): Boolean {
        val product = getProductByRemoteId(site, remoteProductId) ?: return false

        // build a new image list containing all the product images except the passed one
        val imageList = ArrayList<WCProductImageModel>()
        product.getImageListOrEmpty().forEach { image ->
            if (image.id != remoteMediaId) {
                imageList.add(image)
            }
        }
        return if (imageList.size == product.getImageListOrEmpty().size) {
            false
        } else {
            updateProductImages(product, imageList) > 0
        }
    }

    fun deleteProduct(site: SiteModel, remoteProductId: Long): Int {
        return WellSql.delete(WCProductModel::class.java)
                .where()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .equals(WCProductModelTable.REMOTE_PRODUCT_ID, remoteProductId)
                .endWhere()
                .execute()
                .also(::triggerProductsUpdateIfNeeded)
    }

    fun getProductShippingClassListForSite(
        localSiteId: Int
    ): List<WCProductShippingClassModel> {
        return WellSql.select(WCProductShippingClassModel::class.java)
                .where().beginGroup()
                .equals(WCProductShippingClassModelTable.LOCAL_SITE_ID, localSiteId)
                .endGroup().endWhere()
                .asModel
    }

    fun getProductShippingClassByRemoteId(
        remoteShippingClassId: Long,
        localSiteId: Int
    ): WCProductShippingClassModel? {
        return WellSql.select(WCProductShippingClassModel::class.java)
                .where().beginGroup()
                .equals(WCProductShippingClassModelTable.REMOTE_SHIPPING_CLASS_ID, remoteShippingClassId)
                .equals(WCProductShippingClassModelTable.LOCAL_SITE_ID, localSiteId)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun deleteProductShippingClassListForSite(site: SiteModel): Int {
        return WellSql.delete(WCProductShippingClassModel::class.java)
                .where()
                .equals(WCProductShippingClassModelTable.LOCAL_SITE_ID, site.id)
                .or()
                .equals(WCProductShippingClassModelTable.LOCAL_SITE_ID, 0) // Should never happen, but sanity cleanup
                .endWhere().execute()
    }

    fun insertOrUpdateProductShippingClassList(shippingClassList: List<WCProductShippingClassModel>): Int {
        var rowsAffected = 0
        executeInTransaction {
            shippingClassList.forEach {
                rowsAffected += insertOrUpdateProductShippingClass(it)
            }
        }
        return rowsAffected
    }

    fun insertOrUpdateProductShippingClass(shippingClass: WCProductShippingClassModel): Int {
        val result = WellSql.select(WCProductShippingClassModel::class.java)
                .where().beginGroup()
                .equals(WCProductShippingClassModelTable.ID, shippingClass.id)
                .or()
                .beginGroup()
                .equals(WCProductShippingClassModelTable.LOCAL_SITE_ID, shippingClass.localSiteId)
                .equals(WCProductShippingClassModelTable.REMOTE_SHIPPING_CLASS_ID, shippingClass.remoteShippingClassId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            // Insert
            WellSql.insert(shippingClass).execute()
            1
        } else {
            // Update
            val oldId = result.id
            WellSql.update(WCProductShippingClassModel::class.java).whereId(oldId)
                    .put(shippingClass, UpdateAllExceptId(WCProductShippingClassModel::class.java)).execute()
        }
    }

    private fun sortCategoriesByName(
        categories: List<WCProductCategoryModel>,
        descending: Boolean
    ): List<WCProductCategoryModel> {
        return if (descending) {
            categories.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
        } else {
            categories.sortedBy { it.name.lowercase(Locale.getDefault()) }
        }
    }

    fun getProductCategoriesForSite(
        site: SiteModel,
        sortType: ProductCategorySorting = DEFAULT_CATEGORY_SORTING
    ): List<WCProductCategoryModel> {
        val sortOrder = when (sortType) {
            NAME_ASC -> SelectQuery.ORDER_ASCENDING
            NAME_DESC -> SelectQuery.ORDER_DESCENDING
        }
        val sortField = when (sortType) {
            NAME_ASC, NAME_DESC -> WCProductCategoryModelTable.NAME
        }
        val categories = WellSql.select(WCProductCategoryModel::class.java)
                .where()
                .equals(WCProductModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .orderBy(sortField, sortOrder)
                .asModel

        return if (sortType == NAME_ASC || sortType == NAME_DESC) {
            sortCategoriesByName(categories, descending = sortType == NAME_DESC)
        } else {
            categories
        }
    }

    fun getProductCategoryByRemoteId(
        localSiteId: Int,
        categoryId: Long
    ): WCProductCategoryModel? {
        return WellSql.select(WCProductCategoryModel::class.java)
                .where()
                .beginGroup()
                .equals(WCProductCategoryModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(WCProductCategoryModelTable.REMOTE_CATEGORY_ID, categoryId)
                .endGroup()
                .endWhere()
                .asModel.firstOrNull()
    }

    fun getProductCategoriesByRemoteIds(
        site: SiteModel,
        categoryIds: List<Long>
    ): List<WCProductCategoryModel> {
        return WellSql.select(WCProductCategoryModel::class.java)
            .where()
            .beginGroup()
            .isIn(WCProductCategoryModelTable.REMOTE_CATEGORY_ID, categoryIds)
            .equals(WCProductCategoryModelTable.LOCAL_SITE_ID, site.id)
            .endGroup().endWhere()
            .asModel
    }

    fun getProductCategoryByNameAndParentId(
        localSiteId: Int,
        categoryName: String,
        parentId: Long
    ): WCProductCategoryModel? {
        return WellSql.select(WCProductCategoryModel::class.java)
                .where()
                .beginGroup()
                .equals(WCProductCategoryModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(WCProductCategoryModelTable.NAME, categoryName)
                .equals(WCProductCategoryModelTable.PARENT, parentId)
                .endGroup()
                .endWhere()
                .asModel.firstOrNull()
    }

    fun insertOrUpdateProductCategories(productCategories: List<WCProductCategoryModel>): Int {
        var rowsAffected = 0
        executeInTransaction {
            productCategories.forEach {
                rowsAffected += insertOrUpdateProductCategory(it)
            }
        }
        return rowsAffected
    }

    fun insertOrUpdateProductCategory(productCategory: WCProductCategoryModel): Int {
        val result = WellSql.select(WCProductCategoryModel::class.java)
                .where().beginGroup()
                .equals(WCProductCategoryModelTable.ID, productCategory.id)
                .or()
                .beginGroup()
                .equals(WCProductCategoryModelTable.REMOTE_CATEGORY_ID, productCategory.remoteCategoryId)
                .equals(WCProductCategoryModelTable.LOCAL_SITE_ID, productCategory.localSiteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            // Insert
            WellSql.insert(productCategory).execute()
            categoriesUpdatesTrigger.tryEmit(Unit)
            1
        } else {
            // Update
            val oldId = result.id
            WellSql.update(WCProductCategoryModel::class.java).whereId(oldId)
                    .put(productCategory, UpdateAllExceptId(WCProductCategoryModel::class.java))
                    .execute()
                    .also(::triggerCategoriesUpdateIfNeeded)
        }
    }

    fun deleteProductCategory(productCategory: WCProductCategoryModel) =
            WellSql.delete(WCProductCategoryModel::class.java)
                .where()
                .equals(WCProductCategoryModelTable.REMOTE_CATEGORY_ID, productCategory.remoteCategoryId)
                .endWhere().execute()

    fun deleteAllProductCategoriesForSite(site: SiteModel): Int {
        return WellSql.delete(WCProductCategoryModel::class.java)
                .where()
                .equals(WCProductCategoryModelTable.LOCAL_SITE_ID, site.id)
                .or()
                .equals(WCProductCategoryModelTable.LOCAL_SITE_ID, 0) // Should never happen, but sanity cleanup
                .endWhere()
                .execute()
                .also(::triggerCategoriesUpdateIfNeeded)
    }

    fun deleteAllProductCategories() = WellSql.delete(WCProductCategoryModel::class.java)
        .execute()
        .also(::triggerCategoriesUpdateIfNeeded)

    fun getProductTagsForSite(
        localSiteId: Int
    ): List<WCProductTagModel> {
        return WellSql.select(WCProductTagModel::class.java)
                .where().beginGroup()
                .equals(WCProductTagModelTable.LOCAL_SITE_ID, localSiteId)
                .endGroup().endWhere()
                .asModel
    }

    fun getProductTagsByNames(
        localSiteId: Int,
        tags: List<String>
    ): List<WCProductTagModel> {
        return WellSql.select(WCProductTagModel::class.java)
                .where().beginGroup()
                .equals(WCProductTagModelTable.LOCAL_SITE_ID, localSiteId)
                .isIn(WCProductModelTable.NAME, tags)
                .endGroup().endWhere()
                .asModel
    }

    fun getProductTagByName(
        localSiteId: Int,
        tagName: String
    ): WCProductTagModel? {
        return WellSql.select(WCProductTagModel::class.java)
                .where().beginGroup()
                .equals(WCProductTagModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(WCProductTagModelTable.NAME, tagName)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun getProductTagByRemoteId(
        remoteTagId: Long,
        localSiteId: Int
    ): WCProductTagModel? {
        return WellSql.select(WCProductTagModel::class.java)
                .where().beginGroup()
                .equals(WCProductTagModelTable.REMOTE_TAG_ID, remoteTagId)
                .equals(WCProductTagModelTable.LOCAL_SITE_ID, localSiteId)
                .endGroup().endWhere()
                .asModel.firstOrNull()
    }

    fun deleteProductTagsForSite(site: SiteModel): Int {
        return WellSql.delete(WCProductTagModel::class.java)
                .where()
                .equals(WCProductTagModelTable.LOCAL_SITE_ID, site.id)
                .or()
                .equals(WCProductTagModelTable.LOCAL_SITE_ID, 0) // Should never happen, but sanity cleanup
                .endWhere().execute()
    }

    fun insertOrUpdateProductTags(tags: List<WCProductTagModel>): Int {
        var rowsAffected = 0
        tags.forEach {
            rowsAffected += insertOrUpdateProductTag(it)
        }
        return rowsAffected
    }

    fun insertOrUpdateProductTag(tag: WCProductTagModel): Int {
        val result = WellSql.select(WCProductTagModel::class.java)
                .where().beginGroup()
                .equals(WCProductTagModelTable.ID, tag.id)
                .or()
                .beginGroup()
                .equals(WCProductTagModelTable.LOCAL_SITE_ID, tag.localSiteId)
                .equals(WCProductTagModelTable.REMOTE_TAG_ID, tag.remoteTagId)
                .endGroup()
                .endGroup().endWhere()
                .asModel.firstOrNull()

        return if (result == null) {
            // Insert
            WellSql.insert(tag).asSingleTransaction(true).execute()
            1
        } else {
            // Update
            val oldId = result.id
            WellSql.update(WCProductTagModel::class.java).whereId(oldId)
                    .put(tag, UpdateAllExceptId(WCProductTagModel::class.java)).execute()
        }
    }

    private fun executeInTransaction(block: () -> Unit) {
        val db = WellSql.giveMeWritableDb()
        db.beginTransaction()

        try {
            block()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun triggerProductsUpdateIfNeeded(affectedRows: Int) {
        if (affectedRows != 0) productsUpdatesTrigger.tryEmit(Unit)
    }

    private fun triggerVariationsUpdateIfNeeded(affectedRows: Int) {
        if (affectedRows != 0) variationsUpdatesTrigger.tryEmit(Unit)
    }

    private fun triggerCategoriesUpdateIfNeeded(affectedRows: Int) {
        if (affectedRows != 0) categoriesUpdatesTrigger.tryEmit(Unit)
    }

    private fun ConditionClauseBuilder<SelectQuery<WCProductModel>>.applyProductFilterOptions(
        filterOptions: Map<ProductFilterOption, String>
    ): ConditionClauseBuilder<SelectQuery<WCProductModel>> {
        if (filterOptions.containsKey(ProductFilterOption.STATUS)) {
            equals(WCProductModelTable.STATUS, filterOptions[ProductFilterOption.STATUS])
        }
        if (filterOptions.containsKey(ProductFilterOption.STOCK_STATUS)) {
            equals(WCProductModelTable.STOCK_STATUS, filterOptions[ProductFilterOption.STOCK_STATUS])
        }
        if (filterOptions.containsKey(ProductFilterOption.TYPE)) {
            equals(WCProductModelTable.TYPE, filterOptions[ProductFilterOption.TYPE])
        }
        if (filterOptions.containsKey(ProductFilterOption.CATEGORY)) {
            // Building a custom filter, because in the table a product's categories are saved as JSON string, e.g:
            // [{"id":1377,"name":"Decor","slug":"decor"},{"id":1374,"name":"Hoodies","slug":"hoodies"}]
            val categoryFilter = "\"id\":${filterOptions[ProductFilterOption.CATEGORY]},"
            contains(WCProductModelTable.CATEGORIES, categoryFilter)
        }
        return this
    }
}
