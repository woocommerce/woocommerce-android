package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.WCProductModel

@Dao
internal abstract class ProductsDao {

    @Query(
        """
            SELECT * FROM ProductEntity
            WHERE localSiteId = :localSiteId
            AND (:status IS NULL OR status = :status)
            AND (:stockStatus IS NULL OR stockStatus = :stockStatus)
            AND (:type IS NULL OR type = :type)
            AND (:category IS NULL OR categories LIKE '%' || :category || '%')
            AND (:excludeSampleProducts = 0 OR isSampleProduct = 0)
            AND (remoteId NOT IN (:excludedProductIds))
            LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
        """
    )
    @Suppress("LongParameterList")
    abstract fun observeProducts(
        localSiteId: Int,
        status: String?,
        stockStatus: String?,
        type: String?,
        category: String?,
        excludeSampleProducts: Boolean,
        limit: Int?,
        excludedProductIds: List<Long>
    ): Flow<List<WCProductModel>>

    @Query(
        """
            SELECT * FROM ProductEntity
            WHERE localSiteId = :localSiteId
            AND remoteId = :remoteProductId
        """
    )
    abstract fun observeProducts(
        localSiteId: Int,
        remoteProductId: Long
    ): Flow<List<WCProductModel>>

    @Query(
        """
            SELECT * FROM ProductEntity
            WHERE localSiteId = :localSiteId
            AND remoteId = :remoteProductId
            LIMIT 1
        """
    )
    abstract suspend fun getProduct(
        localSiteId: Int,
        remoteProductId: Long
    ): WCProductModel?

    @Query(
        """
            SELECT * FROM ProductEntity
            WHERE localSiteId = :localSiteId
            AND sku = :sku
            LIMIT 1
        """
    )
    abstract suspend fun getProduct(
        localSiteId: Int,
        sku: String
    ): WCProductModel?

    @Query(
        """
            SELECT * FROM ProductEntity
            WHERE localSiteId = :localSiteId
            AND remoteId IN (:remoteProductIds)
            AND (:virtual IS NULL OR virtual = :virtual)
        """
    )
    abstract suspend fun getProducts(
        localSiteId: Int,
        remoteProductIds: List<Long>,
        virtual: Boolean? = null
    ): List<WCProductModel>

    @Upsert
    abstract suspend fun upsertProduct(product: WCProductModel)

    @Upsert
    abstract suspend fun upsertProducts(products: List<WCProductModel>)

    @Query(
        """
            DELETE FROM ProductEntity
            WHERE localSiteId = :localSiteId
    """
    )
    abstract suspend fun deleteProducts(localSiteId: Int)

    @Query(
        """
            DELETE FROM ProductEntity
            WHERE localSiteId = :localSiteId
            AND remoteId = :remoteProductId
    """
    )
    abstract suspend fun deleteProduct(
        localSiteId: Int,
        remoteProductId: Long
    )
}
