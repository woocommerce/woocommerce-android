package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.WCProductModel

@Dao
abstract class ProductsDao {

    @Query(
        """
            SELECT COUNT(*) FROM WCProductModel
            WHERE localSiteId = :localSiteId
            AND (:status IS NULL OR status = :status)
            AND (:stockStatus IS NULL OR stockStatus = :stockStatus)
            AND (:type IS NULL OR type = :type)
            AND (:category IS NULL OR categories LIKE '%' || :category || '%')
            AND (:excludeSampleProducts IS NULL OR isSampleProduct = :excludeSampleProducts)
        """
    )
    abstract fun observeProductsCount(
        localSiteId: Int,
        status: String?,
        stockStatus: String?,
        type: String?,
        category: String?,
        excludeSampleProducts: Boolean
    ): Flow<Long>

    @Query(
        """
            SELECT * FROM WCProductModel
            WHERE localSiteId = :localSiteId
            AND (:status IS NULL OR status = :status)
            AND (:stockStatus IS NULL OR stockStatus = :stockStatus)
            AND (:type IS NULL OR type = :type)
            AND (:category IS NULL OR categories LIKE '%' || :category || '%')
            AND (:excludeSampleProducts IS NULL OR isSampleProduct = :excludeSampleProducts)
            AND (:excludedProductIds IS NULL OR remoteProductId NOT IN (:excludedProductIds))
            ORDER BY :sortField :sortOrder COLLATE NOCASE
            LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
        """
    )
    abstract fun observeProducts(
        localSiteId: Int,
        status: String?,
        stockStatus: String?,
        type: String?,
        category: String?,
        excludeSampleProducts: Boolean,
        sortField: String,
        sortOrder: String,
        limit: Int?,
        excludedProductIds: List<Long>? = null
    ): Flow<List<WCProductModel>>

    @Query(
        """
            SELECT * FROM WCProductModel
            WHERE localSiteId = :localSiteId
            AND remoteProductId = :remoteProductId
        """
    )
    abstract fun observeProducts(
        localSiteId: Int,
        remoteProductId: Long
    ): Flow<List<WCProductModel>>

    @Query(
        """
            SELECT * FROM WCProductModel
            WHERE localSiteId = :localSiteId
            AND remoteProductId = :remoteProductId
            LIMIT 1
        """
    )
    abstract suspend fun getProduct(
        localSiteId: Int,
        remoteProductId: Long
    ): WCProductModel?

    @Query(
        """
            SELECT * FROM WCProductModel
            WHERE localSiteId = :localSiteId
            AND remoteProductId IN (:remoteProductIds)
            AND (:virtual IS NULL OR virtual = :virtual)
        """
    )
    abstract suspend fun getProducts(
        localSiteId: Int,
        remoteProductIds: List<Long>,
        virtual: Boolean? = null
    ): List<WCProductModel>

    //TODO: Validate "rowsAffected" case: its not "affected rows" but rather affected sqlite row id
    @Upsert
    abstract fun upsertProduct(product: WCProductModel): Int

    @Upsert
    abstract fun upsertProducts(products: List<WCProductModel>): Int

}
