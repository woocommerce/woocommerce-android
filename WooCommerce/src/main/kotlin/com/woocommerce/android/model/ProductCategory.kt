package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.ui.products.categories.ProductCategoryItemUiModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductCategoryModel
import java.util.Locale
import java.util.Stack

@Parcelize
data class ProductCategory(
    val remoteCategoryId: Long = 0L,
    val name: String,
    val slug: String = "",
    val parentId: Long = 0L
) : Parcelable {
    companion object {
        const val DEFAULT_PRODUCT_CATEGORY_MARGIN = 32
    }

    fun toProductCategory(): ProductCategory {
        return ProductCategory(
            this.remoteCategoryId,
            this.name,
            this.slug
        )
    }

    /**
     * Computes the cascading margin for the category name according to its parent
     *
     * @param hierarchy the map of parent to child relationship
     * the [ProductCategory] for which the padding is being calculated
     *
     * @return Int the computed margin
     */
    fun computeCascadingMargin(hierarchy: Map<Long, Long>): Int {
        var margin = DEFAULT_PRODUCT_CATEGORY_MARGIN
        var parent = this.parentId
        while (parent != 0L) {
            margin += DEFAULT_PRODUCT_CATEGORY_MARGIN
            parent = hierarchy[parent] ?: 0L
        }
        return margin
    }
}

/**
 * Returns true if the passed category is in the current list of categories
 */
fun List<ProductCategory>.containsCategory(category: ProductCategory): Boolean {
    this.forEach {
        if (category.remoteCategoryId == it.remoteCategoryId) {
            return true
        }
    }
    return false
}

/**
 * The method does a Depth First Traversal of the Product Categories and returns an ordered list, which
 * is grouped by their Parent id. The sort is stable, which means that it should return the same list
 * when new categories are updated, and the sort is relative to the update.
 *
 * @return [Set<ProductCategoryItemUiModel>] a sorted set of view holder models containing category view data
 */
private fun List<ProductCategory>.sortByNameAndParent(): Set<ProductCategoryItemUiModel> {
    val sortedList = mutableSetOf<ProductCategoryItemUiModel>()
    val stack = Stack<ProductCategory>()
    val visited = mutableSetOf<Long>()

    // we first sort the list by name in a descending order
    val productCategoriesSortedByNameDesc = this.sortedByDescending { it.name.toLowerCase(Locale.US) }

    // add root nodes to the Stack
    stack.addAll(productCategoriesSortedByNameDesc.filter { it.parentId == 0L })

    // Go through the nodes until we've finished DFS
    while (stack.isNotEmpty()) {
        val category = stack.pop()
        // Do not revisit a category
        if (!visited.contains(category.remoteCategoryId)) {
            visited.add(category.remoteCategoryId)
            sortedList.add(ProductCategoryItemUiModel(category))
        }

        // Find all children of the node from the main category list
        val children = productCategoriesSortedByNameDesc.filter {
            it.parentId == category.remoteCategoryId
        }
        if (!children.isNullOrEmpty()) {
            stack.addAll(children)
        }
    }
    return sortedList
}

/**
 * The method takes in a list of product categories and calculates the order and grouping of categories
 * by their parent ids. This creates a stable sorted list of categories by name. The returned list also
 * has margin data, which can be used to visually represent categories in a hierarchy under their
 * parent ids.
 *
 * @return [List<ProductCategoryItemUiModel>] the sorted styled list of categories
 */
fun List<ProductCategory>.sortCategories(): List<ProductCategoryItemUiModel> {
    val parentChildMap = mutableMapOf<Long, Long>()

    // Build a parent child relationship table
    for (category in this) {
        parentChildMap[category.remoteCategoryId] = category.parentId
    }

    // Sort all incoming categories by their parent
    val sortedList = this.sortByNameAndParent()

    // Update the margin of the category
    for (categoryViewHolderModel in sortedList) {
        categoryViewHolderModel.margin = categoryViewHolderModel.category.computeCascadingMargin(parentChildMap)
    }
    return sortedList.toList()
}

fun WCProductCategoryModel.toProductCategory(): ProductCategory {
    return ProductCategory(
        remoteCategoryId = this.remoteCategoryId,
        name = this.name,
        slug = this.slug,
        parentId = this.parent
    )
}
