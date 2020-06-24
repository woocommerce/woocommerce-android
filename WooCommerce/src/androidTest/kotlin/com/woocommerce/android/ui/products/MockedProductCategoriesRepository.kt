package com.woocommerce.android.ui.products

import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.toProductCategory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import org.wordpress.android.fluxc.model.WCProductModel.ProductTriplet
import org.wordpress.android.fluxc.store.WCProductStore

class MockedProductCategoriesRepository constructor(
    dispatcher: Dispatcher,
    productStore: WCProductStore,
    selectedSite: SelectedSite
) : ProductCategoriesRepository(
    dispatcher,
    productStore,
    selectedSite
) {
    var categoriesList: List<ProductCategory>? = null

    override var canLoadMoreProductCategories: Boolean
        get() = false
        set(value) {}

    override fun getProductCategoriesList(): List<ProductCategory> {
        return categoriesList ?: generateTestProductCategoriesAppModelList()
    }

    suspend fun fetchProductCategories(pageSize: Int, loadMore: Boolean): List<ProductCategory> {
        return generateTestProductCategoriesAppModelList()
    }

    suspend fun fetchProductCategories(): List<ProductCategory> {
        return generateTestProductCategoriesAppModelList()
    }

    private fun generateTestProductCategoriesList(): List<WCProductCategoryModel> {
        val categoriesList = ArrayList<WCProductCategoryModel>()
        with(WCProductCategoryModel(1)) {
            remoteCategoryId = 1
            parent = 0L
            slug = ""
            name = ""
            categoriesList.add(this)
        }
        with(WCProductCategoryModel(2)) {
            remoteCategoryId = 2
            parent = 0L
            slug = ""
            name = ""
            categoriesList.add(this)
        }
        return categoriesList
    }

    private fun generateTestProductCategoriesTripletList(): List<ProductTriplet> {
        return generateTestProductCategoriesList().map {
            ProductTriplet(
                it.remoteCategoryId,
                it.name,
                it.slug
            )
        }
    }

    private fun generateTestProductCategoriesAppModelList(): List<ProductCategory> {
        return generateTestProductCategoriesList().map { it.toProductCategory() }
    }
}
