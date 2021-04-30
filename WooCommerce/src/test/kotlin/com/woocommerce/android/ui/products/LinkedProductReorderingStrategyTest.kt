package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import org.junit.Assert
import org.junit.Test

class LinkedProductReorderingStrategyTest {
    @Test
    fun `re-ordering linked product from 1st position to 2nd, the product list order must be correct`() {
        // Given
        val currentLinkedProductList = ProductTestUtils.generateProductList()
        val productListSwappingStrategy = DefaultProductListReorderingStrategy()
        val expectedLinkedProductIds = arrayListOf(2L, 1L, 3L, 4L, 5L)

        // When

        // Imitate dragging product from 0 to 1st position
        productListSwappingStrategy.reOrderItems(0, 1, currentLinkedProductList as ArrayList<Product>)

        val actualLinkedProductIds = currentLinkedProductList.map {
            it.remoteId
        }

        // Then
        Assert.assertEquals(expectedLinkedProductIds, actualLinkedProductIds)
    }

    @Test
    fun `re-ordering product from 1st position to 2nd,and 2nd to 5th position,the order must be correct`() {
        // Given
        val currentLinkedProductList = ProductTestUtils.generateProductList()
        val productListSwappingStrategy = DefaultProductListReorderingStrategy()
        val expectedLinkedProductIds = arrayListOf(2L, 3L, 4L, 5L, 1L)

        // When

        // Imitate dragging product 1 from 1st to 2nd position
        productListSwappingStrategy.reOrderItems(0, 1, currentLinkedProductList as ArrayList<Product>)
        // Imitate dragging product 2 from 2nd to 5th position
        productListSwappingStrategy.reOrderItems(1, 2, currentLinkedProductList)
        productListSwappingStrategy.reOrderItems(2, 3, currentLinkedProductList)
        productListSwappingStrategy.reOrderItems(3, 4, currentLinkedProductList)

        val actualLinkedProductIds = currentLinkedProductList.map {
            it.remoteId
        }

        // Then
        Assert.assertEquals(expectedLinkedProductIds, actualLinkedProductIds)
    }
}
