package org.wordpress.android.fluxc.domain

data class GlobalAddonGroup(
    val name: String,
    val restrictedCategoriesIds: CategoriesRestriction,
    val addons: List<Addon>
) {
    sealed class CategoriesRestriction {
        object AllProductsCategories : CategoriesRestriction()
        data class SpecifiedProductCategories(val productCategories: List<Long>) : CategoriesRestriction()
    }
}
