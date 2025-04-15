package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
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

}
