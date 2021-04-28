package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import org.junit.Assert
import org.junit.Test

class LinkedProductSwappingStrategyTest {

    @Test
    fun `after drag-and-drop of linked product from 1st to 2nd position the product list order must be correct` () {

        // Given
        val currentLinkedProductList = ProductTestUtils.generateProductList()
        val productListSwappingStrategy = DefaultProductListSwappingStrategy()
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
    fun `after drag-and-drop of linked product from 1st to 2nd and 2nd to 4th position, the product list order must be correct` () {

        // Given
        val currentLinkedProductList = ProductTestUtils.generateProductList()
        val productListSwappingStrategy = DefaultProductListSwappingStrategy()
        val expectedLinkedProductIds = arrayListOf(2L, 1L, 4L, 3L, 5L)


        // When

        // Imitate dragging product 1 from 1st to 3rd position
        productListSwappingStrategy.reOrderItems(0, 1, currentLinkedProductList as ArrayList<Product>)
        // Imitate dragging product 2 from 2nd to 4th position
        productListSwappingStrategy.reOrderItems(1, 2, currentLinkedProductList)
        productListSwappingStrategy.reOrderItems(1, 2, currentLinkedProductList)
        productListSwappingStrategy.reOrderItems(2, 3, currentLinkedProductList)

        val actualLinkedProductIds = currentLinkedProductList.map {
            it.remoteId
        }

        // Then
        Assert.assertEquals(expectedLinkedProductIds, actualLinkedProductIds)
    }
}
