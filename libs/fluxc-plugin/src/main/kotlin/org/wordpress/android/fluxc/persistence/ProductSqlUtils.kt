@file:OptIn(ExperimentalCoroutinesApi::class)

package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wellsql.generated.WCProductCategoryModelTable
import com.wellsql.generated.WCProductReviewModelTable
import com.wellsql.generated.WCProductShippingClassModelTable
import com.wellsql.generated.WCProductTagModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCBundledProduct
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductComponent
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.persistence.dao.ProductsDao
import org.wordpress.android.fluxc.store.WCProductStore.Companion.DEFAULT_CATEGORY_SORTING
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting.NAME_ASC
import org.wordpress.android.fluxc.store.WCProductStore.ProductCategorySorting.NAME_DESC
import java.util.Locale

@Suppress("LargeClass")
internal object ProductSqlUtils {
    private const val DEBOUNCE_DELAY_FOR_OBSERVERS = 50L
    private val categoriesUpdatesTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val gson by lazy { Gson() }

    fun observeCategories(site: SiteModel, sortType: ProductCategorySorting): Flow<List<WCProductCategoryModel>> {
        return categoriesUpdatesTrigger
            .onStart { emit(Unit) }
            .debounce(DEBOUNCE_DELAY_FOR_OBSERVERS)
            .mapLatest {
                getProductCategoriesForSite(site, sortType)
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun ProductsDao.getCompositeProducts(site: SiteModel, remoteProductId: Long): List<WCProductComponent> {
        val productModel = getProduct(site.id, remoteProductId)

        return productModel?.let {
            val responseType = object : TypeToken<List<WCProductComponent>>() {}.type
            gson.fromJson(it.compositeComponents, responseType) as? List<WCProductComponent>
        } ?: emptyList()
    }

    private fun getBundledProducts(
        productModel: WCProductModel?,
    ): List<WCBundledProduct> {
        return productModel?.let {
            val responseType = object : TypeToken<List<WCBundledProduct>>() {}.type
            gson.fromJson(it.bundledItems, responseType) as? List<WCBundledProduct>
        } ?: emptyList()
    }

    fun ProductsDao.observeBundledProducts(
        site: SiteModel,
        remoteProductId: Long
    ): Flow<List<WCBundledProduct>> {
        return observeProducts(
            localSiteId = site.id,
            remoteProductId = remoteProductId
        ).mapLatest { product ->
            getBundledProducts(product.firstOrNull())
        }
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
            .equals(WCProductCategoryModelTable.LOCAL_SITE_ID, site.id)
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
            .isIn(WCProductTagModelTable.NAME, tags)
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

    private fun triggerCategoriesUpdateIfNeeded(affectedRows: Int) {
        if (affectedRows != 0) categoriesUpdatesTrigger.tryEmit(Unit)
    }
}
