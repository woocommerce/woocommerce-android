package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.store.WCProductStore

@Dao
internal abstract class ProductCategoriesDao {

    private companion object {
        const val GET_FOR_SITE_QUERY = """
           SELECT * FROM ProductCategoryEntity
           WHERE localSiteId = :siteId
           ORDER BY
               CASE WHEN :sortType = 'NAME_ASC' THEN name COLLATE NOCASE END ASC,
               CASE WHEN :sortType = 'NAME_DESC' THEN name COLLATE NOCASE END DESC
        """

    }

    @Query(GET_FOR_SITE_QUERY)
    abstract suspend fun getProductCategories(
        siteId: LocalId,
        sortType: WCProductStore.ProductCategorySorting
    ): List<WCProductCategoryModel>

    @Query(GET_FOR_SITE_QUERY)
    abstract fun observeProductCategories(
        siteId: LocalId,
        sortType: WCProductStore.ProductCategorySorting
    ): Flow<List<WCProductCategoryModel>>

    @Query(
        """
            SELECT * FROM ProductCategoryEntity
            WHERE localSiteId = :siteId
            AND remoteCategoryId = :remoteCategoryId
        """
    )
    abstract suspend fun getProductCategory(
        siteId: LocalId,
        remoteCategoryId: RemoteId
    ): WCProductCategoryModel?

    @Query(
        """
            SELECT * FROM ProductCategoryEntity
            WHERE localSiteId = :siteId
            AND remoteCategoryId IN (:categoryIds)
        """
    )
    abstract suspend fun getProductCategories(
        siteId: LocalId,
        categoryIds: List<RemoteId>
    ): List<WCProductCategoryModel>

    @Upsert
    abstract suspend fun upsertProductCategory(productCategory: WCProductCategoryModel)

    @Upsert
    abstract suspend fun upsertProductCategories(productCategories: List<WCProductCategoryModel>)

    @Delete
    abstract suspend fun deleteProductCategory(productCategory: WCProductCategoryModel)

    @Query(
        """
            DELETE FROM ProductCategoryEntity
            WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun deleteProductCategoriesForSite(siteId: LocalId)

    @Query(
        """
            DELETE FROM ProductCategoryEntity
        """
    )
    abstract suspend fun deleteAllProductCategories()
}
