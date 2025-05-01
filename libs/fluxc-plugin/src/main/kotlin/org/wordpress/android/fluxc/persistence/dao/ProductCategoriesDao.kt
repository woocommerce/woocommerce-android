package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.store.WCProductStore

@Dao
internal abstract class ProductCategoriesDao {

    companion object {
        const val DEFAULT_SELECT_QUERY = """
           SELECT * FROM ProductCategoryEntity
           WHERE localSiteId = :localSiteId
           ORDER BY
               CASE WHEN :sortType = 'NAME_ASC' THEN name COLLATE NOCASE END ASC,
               CASE WHEN :sortType = 'NAME_DESC' THEN name COLLATE NOCASE END DESC
        """
    }

    @Query(DEFAULT_SELECT_QUERY)
    abstract suspend fun getProductCategories(
        localSiteId: Int,
        sortType: WCProductStore.ProductCategorySorting
    ): List<WCProductCategoryModel>

    @Query(DEFAULT_SELECT_QUERY)
    abstract fun observeProductCategories(
        localSiteId: Int,
        sortType: WCProductStore.ProductCategorySorting
    ): Flow<List<WCProductCategoryModel>>

}
