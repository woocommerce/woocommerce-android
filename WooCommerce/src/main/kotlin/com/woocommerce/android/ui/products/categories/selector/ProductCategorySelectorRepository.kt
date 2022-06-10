package com.woocommerce.android.ui.products.categories.selector

import com.woocommerce.android.WooException
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.toProductCategory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.categories.ProductCategoriesFragment
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

/**
 * This class seems like a duplicate of [ProductCategoriesRepository].
 * The goal here is to make this as the main repository after adding having features parity between
 * [ProductCategorySelectorFragment] and [ProductCategoriesFragment], and then remove the old one that depends
 * on EventBus.
 */
class ProductCategorySelectorRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: WCProductStore
) {
    fun observeCategories(): Flow<List<ProductCategory>> = store.observeCategories(
        site = selectedSite.get()
    ).map {
        it.map(WCProductCategoryModel::toProductCategory)
    }

    suspend fun fetchCategories(
        offset: Int,
        pageSize: Int
    ): Result<Boolean> {
        return store.fetchProductCategories(selectedSite.get(), offset, pageSize)
            .let { result ->
                if (result.isError) {
                    WooLog.w(
                        WooLog.T.PRODUCTS,
                        "Fetching categories failed, error: ${result.error.type}: ${result.error.message}"
                    )
                    Result.failure(WooException(result.error))
                } else {
                    Result.success(result.model!!)
                }
            }
    }

    suspend fun searchCategories(
        searchQuery: String,
        offset: Int,
        pageSize: Int
    ): Result<SearchResult> {
        return store.searchProductCategories(
            selectedSite.get(),
            searchString = searchQuery,
            offset = offset,
            pageSize = pageSize
        ).let { result ->
            if (result.isError) {
                WooLog.w(
                    WooLog.T.PRODUCTS,
                    "Searching product categories failed, error: ${result.error.type}: ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            } else {
                val searchResult = result.model!!
                Result.success(
                    SearchResult(
                        productCategories = searchResult.categories
                            .map { categoryDataModel -> categoryDataModel.toProductCategory() },
                        canLoadMore = searchResult.canLoadMore
                    )
                )
            }
        }
    }

    data class SearchResult(
        val productCategories: List<ProductCategory>,
        val canLoadMore: Boolean
    )
}
