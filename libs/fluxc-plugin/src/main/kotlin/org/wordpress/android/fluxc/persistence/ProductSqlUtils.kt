@file:OptIn(ExperimentalCoroutinesApi::class)

package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wellsql.generated.WCProductReviewModelTable
import com.wellsql.generated.WCProductShippingClassModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCBundledProduct
import org.wordpress.android.fluxc.model.WCProductComponent
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductReviewModel
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import org.wordpress.android.fluxc.model.WCProductTagModel
import org.wordpress.android.fluxc.persistence.dao.ProductsDao

@Suppress("LargeClass")
internal object ProductSqlUtils {
    private val gson by lazy { Gson() }

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
}
