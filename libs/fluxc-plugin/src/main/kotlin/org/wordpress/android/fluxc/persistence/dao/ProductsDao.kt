package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore

@Dao
@Suppress("LongParameterList")
internal abstract class ProductsDao {

    companion object {
        const val DEFAULT_SELECT_QUERY = """
            SELECT * FROM ProductEntity
            WHERE localSiteId = :localSiteId
            AND (:status IS NULL OR status = :status)
            AND (:stockStatus IS NULL OR stockStatus = :stockStatus)
            AND (:type IS NULL OR type = :type)
            AND (:category IS NULL OR categories LIKE '%' || :category || '%')
            AND (:excludeSampleProducts = 0 OR isSampleProduct = 0)
            AND (remoteId NOT IN (:excludedProductIds))
            ORDER BY
                CASE WHEN :sortType = 'TITLE_ASC' THEN name COLLATE NOCASE END ASC,
                CASE WHEN :sortType = 'TITLE_DESC' THEN name COLLATE NOCASE END DESC,
                CASE WHEN :sortType = 'DATE_ASC' THEN dateCreated END ASC,
                CASE WHEN :sortType = 'DATE_DESC' THEN dateCreated END DESC,
                CASE WHEN :sortType = 'POPULARITY_ASC' THEN totalSales END ASC,
                CASE WHEN :sortType = 'POPULARITY_DESC' THEN totalSales END DESC
            LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
        """
    }

    @Query(DEFAULT_SELECT_QUERY)
    abstract fun observeProducts(
        localSiteId: Int,
        status: String?,
        stockStatus: String?,
        type: String?,
        category: String?,
        excludeSampleProducts: Boolean,
        limit: Int?,
        excludedProductIds: List<Long>,
        sortType: WCProductStore.ProductSorting
    ): Flow<List<WCProductModel>>

    @Query(DEFAULT_SELECT_QUERY)
    abstract suspend fun getProducts(
        localSiteId: Int,
        status: String?,
        stockStatus: String?,
        type: String?,
        category: String?,
        excludeSampleProducts: Boolean,
        limit: Int?,
        excludedProductIds: List<Long>,
        sortType: WCProductStore.ProductSorting
    ): List<WCProductModel>

    @Query(
        """
            SELECT COUNT(*) FROM ProductEntity
            WHERE localSiteId = :localSiteId
            AND (:status IS NULL OR status = :status)
            AND (:stockStatus IS NULL OR stockStatus = :stockStatus)
            AND (:type IS NULL OR type = :type)
            AND (:category IS NULL OR categories LIKE '%' || :category || '%')
            AND (:excludeSampleProducts = 0 OR isSampleProduct = 0)
        """
    )
    abstract fun observeProductsCount(
        localSiteId: Int,
        status: String?,
        stockStatus: String?,
        type: String?,
        category: String?,
        excludeSampleProducts: Boolean,
    ): Flow<Long>

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

    @Query(
        """
        SELECT * FROM ProductEntity
        WHERE localSiteId = :localSiteId
        AND (
            name LIKE '%' || :searchQuery || '%'
            OR description LIKE '%' || :searchQuery || '%'
            OR shortDescription LIKE '%' || :searchQuery || '%'
        )
        """
    )
    abstract suspend fun searchProductsByQuery(
        localSiteId: Int,
        searchQuery: String
    ): List<WCProductModel>

    @Query(
        """
        SELECT * FROM ProductEntity
        WHERE localSiteId = :localSiteId
        AND :sku IS sku
        ORDER BY name ASC
        """
    )
    abstract suspend fun searchProductsBySkuExactMatch(
        localSiteId: Int,
        sku: String?
    ): List<WCProductModel>

    @Query(
        """
        SELECT * FROM ProductEntity
        WHERE localSiteId = :localSiteId
        AND :sku LIKE '%' || sku || '%'
        ORDER BY name ASC
        """
    )
    abstract suspend fun searchProductsBySkuPartialMatch(
        localSiteId: Int,
        sku: String?
    ): List<WCProductModel>

}
