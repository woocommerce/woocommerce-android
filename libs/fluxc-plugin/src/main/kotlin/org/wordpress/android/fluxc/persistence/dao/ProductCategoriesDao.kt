package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.store.WCProductStore

@Dao
internal abstract class ProductCategoriesDao {

    private companion object {
        const val GET_FOR_SITE_QUERY = """
           SELECT * FROM ProductCategoryEntity
           WHERE localSiteId = :localSiteId
           ORDER BY
               CASE WHEN :sortType = 'NAME_ASC' THEN name COLLATE NOCASE END ASC,
               CASE WHEN :sortType = 'NAME_DESC' THEN name COLLATE NOCASE END DESC
        """

    }

    @Query(GET_FOR_SITE_QUERY)
    abstract suspend fun getProductCategories(
        localSiteId: Int,
        sortType: WCProductStore.ProductCategorySorting
    ): List<WCProductCategoryModel>

    @Query(GET_FOR_SITE_QUERY)
    abstract fun observeProductCategories(
        localSiteId: Int,
        sortType: WCProductStore.ProductCategorySorting
    ): Flow<List<WCProductCategoryModel>>

    @Query(
        """
            SELECT * FROM ProductCategoryEntity
            WHERE localSiteId = :localSiteId
            AND remoteCategoryId = :remoteCategoryId
        """
    )
    abstract suspend fun getProductCategory(
        localSiteId: Int,
        remoteCategoryId: Long
    ): WCProductCategoryModel?

    @Query(
        """
            SELECT * FROM ProductCategoryEntity
            WHERE localSiteId = :localSiteId
            AND remoteCategoryId IN (:categoryIds)
        """
    )
    abstract suspend fun getProductCategories(
        localSiteId: Int,
        categoryIds: List<Long>
    ): List<WCProductCategoryModel>

}
