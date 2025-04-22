package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.ProductSqlUtils.getProducts
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.wc.product.ProductTestUtils
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class ProductsDaoTest {
    private lateinit var sut: ProductsDao
    private lateinit var database: WCAndroidDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sut = database.productsDao
    }

    @Test
    fun testInsertOrUpdateProduct() = runTest {
        val productModel = ProductTestUtils.generateSampleProduct(40)
        val site = SiteModel().apply { id = productModel.localSiteId.value }

        // Test inserting product

        sut.upsertProduct(productModel)
        val storedProductsCount = sut.getAllProductsForSite(site.id).count()
        assertEquals(1, storedProductsCount)

        // Test updating product
        val storedProduct = sut.getProduct(site.id, productModel.remoteProductId)?.copy(
            name = "Anitaa Test",
            virtual = true
        )
        storedProduct?.also {
            sut.upsertProduct(it)
        }

        val updatedProductsCount = sut.getAllProductsForSite(site.id).count()
        assertEquals(1, updatedProductsCount)

        val updatedProduct = sut.getProduct(site.id, productModel.remoteProductId)
        assertEquals(storedProduct?.remoteProductId, updatedProduct?.remoteProductId)
        assertEquals(storedProduct?.name, updatedProduct?.name)
        assertEquals(storedProduct?.virtual, updatedProduct?.virtual)
    }

    @Test
    fun testInsertOrUpdateProductWithDecimalQuantity() = runTest {
        val productModel = ProductTestUtils.generateSampleProduct(
            remoteId = 42,
            stockQuantity = 4.2
        )
        val site = SiteModel().apply { id = productModel.localSiteId.value }

        // Test inserting product
        sut.upsertProduct(productModel)
        val storedProductsCount = sut.getProducts(site.id, listOf(productModel.remoteProductId)).count()
        assertEquals(1, storedProductsCount)

        // Test updating product
        val storedProduct = sut.getProduct(site.id, productModel.remoteProductId)?.copy(stockQuantity = 4.0)
        storedProduct?.also {
            sut.upsertProduct(it)
        }

        val updatedProductsCount = sut.getAllProductsForSite(site.id).count()
        assertEquals(1, updatedProductsCount)

        val updatedProduct = sut.getProduct(site.id, productModel.remoteProductId)
        assertEquals(storedProduct?.remoteProductId, updatedProduct?.remoteProductId)
        assertEquals(storedProduct?.name, updatedProduct?.name)
        assertEquals(storedProduct?.virtual, updatedProduct?.virtual)
    }

    @Test
    fun testInsertOrUpdateProducts() = runTest {
        val site = SiteModel().apply { id = 2 }
        val products = ArrayList<WCProductModel>().apply {
            this.add(ProductTestUtils.generateSampleProduct(40, siteId = site.id))
            this.add(ProductTestUtils.generateSampleProduct(41, siteId = site.id))
            this.add(ProductTestUtils.generateSampleProduct(42, siteId = site.id))
        }

        // Delete all products for this site, then test inserting the above products
        sut.deleteProducts(site.id)
        sut.upsertProducts(products)
        val storedProducts = sut.getAllProductsForSite(site.id)
        assertEquals(products, storedProducts)
    }

    @Test
    fun testGetProductsForSite() = runTest {
        // insert products for one site
        val site1 = SiteModel().apply { id = 2 }
        val product1 = ProductTestUtils.generateSampleProduct(40, siteId = site1.id)
        sut.upsertProduct(product1)

        // verify that it is stored
        val storedProduct = sut.getProduct(site1.id, product1.remoteProductId)
        assertEquals(product1.remoteProductId, storedProduct?.remoteProductId)
        assertEquals(product1.name, storedProduct?.name)
        assertEquals(product1.virtual, storedProduct?.virtual)

        // insert products for another site
        val site2 = SiteModel().apply { id = 10 }
        val product2 = ProductTestUtils.generateSampleProduct(43, siteId = site2.id)
        sut.upsertProduct(product2)

        // verify that it is stored
        val storedProduct2 = sut.getProduct(site2.id, product2.remoteProductId)
        assertEquals(product2.remoteProductId, storedProduct2?.remoteProductId)
        assertEquals(product2.name, storedProduct2?.name)
        assertEquals(product2.virtual, storedProduct2?.virtual)

        // add another product for site 1
        val product3 = ProductTestUtils.generateSampleProduct(43, siteId = site1.id)
        sut.upsertProduct(product3)

        // verify that the site 2 product size is still the same
        val storedProductForSite2Count = sut.getAllProductsForSite(site2.id).count()
        assertEquals(1, storedProductForSite2Count)

        // verify that the site 1 product is increases by 1
        val storedProductForSite1Count = sut.getAllProductsForSite(site1.id).count()
        assertEquals(2, storedProductForSite1Count)
    }

    @Test
    fun testGetVirtualProductsForSite() = runTest {
        // insert products for one site
        val site1 = SiteModel().apply { id = 2 }
        val products = ArrayList<WCProductModel>().apply {
            this.add(ProductTestUtils.generateSampleProduct(40, siteId = site1.id, virtual = true))
            this.add(ProductTestUtils.generateSampleProduct(41, siteId = site1.id, virtual = false))
            this.add(ProductTestUtils.generateSampleProduct(42, siteId = site1.id, virtual = true))
        }

        sut.upsertProducts(products)

        // verify that the product list exists locally
        val remoteProductIds = products.map { it.remoteProductId }.toList()
        val storedProductCountForSite1 = sut.getProducts(site1.id, remoteProductIds).count()
        assertEquals(remoteProductIds.size, storedProductCountForSite1)

        // verify that only 2 of the products are virtual
        assertEquals(2, sut.getProducts(site1.id, remoteProductIds, virtual = true).count())

        // insert products for another site
        val site2 = SiteModel().apply { id = 10 }
        val products2 = ArrayList<WCProductModel>().apply {
            this.add(ProductTestUtils.generateSampleProduct(40, siteId = site2.id, virtual = true))
            this.add(ProductTestUtils.generateSampleProduct(41, siteId = site2.id, virtual = true))
            this.add(ProductTestUtils.generateSampleProduct(42, siteId = site2.id, virtual = true))
        }
        sut.upsertProducts(products2)

        // verify that it is stored
        val remoteProductIds2 = products2.map { it.remoteProductId }.toList()
        val storedProductCountForSite2 = sut.getProducts(site2.id, remoteProductIds2).count()
        assertEquals(remoteProductIds2.size, storedProductCountForSite2)

        // verify that all of the products are virtual
        assertEquals(3, sut.getProducts(site2.id, remoteProductIds2, virtual = true).count())

        // insert products for another site
        val site3 = SiteModel().apply { id = 11 }
        val products3 = ArrayList<WCProductModel>().apply {
            this.add(ProductTestUtils.generateSampleProduct(40, siteId = site3.id))
            this.add(ProductTestUtils.generateSampleProduct(41, siteId = site3.id))
            this.add(ProductTestUtils.generateSampleProduct(42, siteId = site3.id))
        }
        sut.upsertProducts(products3)

        // verify that it is stored
        val remoteProductIds3 = products3.map { it.remoteProductId }.toList()
        val storedProductCountForSite3 = sut.getProducts(site3.id, remoteProductIds3).count()
        assertEquals(remoteProductIds3.size, storedProductCountForSite3)

        // verify that none of the products are virtual
        assertEquals(0, sut.getProducts(site3.id, remoteProductIds3, virtual = true).count())
    }

    @Test
    fun testDeleteProduct() = runTest {
        val remoteProductId = 40L
        val productModel = ProductTestUtils.generateSampleProduct(remoteProductId)
        val site = SiteModel().apply { id = productModel.localSiteId.value }

        // Test inserting product
        sut.upsertProduct(productModel)
        val storedProductsCount = sut.getAllProductsForSite(site.id).count()
        assertEquals(1, storedProductsCount)

        // Test deleting product
        sut.deleteProduct(site.id, remoteProductId)
        assertNull(sut.getProduct(site.id, remoteProductId))
    }

    @Test
    fun testGetProductsForSiteAndProductIds() = runTest {
        val productIds = listOf<Long>(40, 41, 2)

        val product1 = ProductTestUtils.generateSampleProduct(40)
        val product2 = ProductTestUtils.generateSampleProduct(41)
        val product3 = ProductTestUtils.generateSampleProduct(42)

        sut.upsertProducts(listOf(product1, product2, product3))

        val site = SiteModel().apply { id = product1.localSiteId.value }
        val products = sut.getProducts(site.id, productIds)
        assertEquals(2, products.size)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(40, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(41, siteId = 10)
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(2, siteId = 10)

        sut.upsertProducts(listOf(differentSiteProduct1, differentSiteProduct2, differentSiteProduct3))

        // verify that the products for the first site is still 2
        assertEquals(2, sut.getProducts(site.id, productIds).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId.value }
        val differentSiteProducts = sut.getProducts(site2.id, productIds)
        assertEquals(3, differentSiteProducts.size)
    }

    @Test
    fun testGetProductsWithFilterOptions() = runTest {
        val productFilterOptions = mapOf(
            ProductFilterOption.STOCK_STATUS to "instock",
            ProductFilterOption.STATUS to "publish",
            ProductFilterOption.TYPE to "simple"
        )

        val product1 = ProductTestUtils.generateSampleProduct(40)
        val product2 = ProductTestUtils.generateSampleProduct(41)
        val product3 = ProductTestUtils.generateSampleProduct(42, stockStatus = "onbackorder")

        sut.upsertProducts(listOf(product1, product2, product3))

        val site = SiteModel().apply { id = product1.localSiteId.value }
        val products = sut.getProducts(site, filterOptions = productFilterOptions)
        assertEquals(2, products.size)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(40, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(
            41, siteId = 10, type = "grouped"
        )
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(
            2, siteId = 10, status = "pending"
        )

        sut.upsertProducts(listOf(differentSiteProduct1, differentSiteProduct2, differentSiteProduct3))

        // verify that the products for the first site is still 2
        assertEquals(2, sut.getProducts(site, productFilterOptions).size)

        // verify that the products for the second site is 3
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId.value }
        val differentSiteProducts = sut.getProducts(site2, productFilterOptions)
        assertEquals(1, differentSiteProducts.size)
    }

    @Test
    fun testGetProductsWithSearchQuery() = runTest {
        val product1 = ProductTestUtils.generateSampleProduct(
            40, name = "a",
            description = "1", shortDescription = "+"
        )
        val product2 = ProductTestUtils.generateSampleProduct(
            41, name = "b",
            description = "2", shortDescription = "-"
        )
        val product3 = ProductTestUtils.generateSampleProduct(
            42, name = "xyz ab piu",
            description = "xyz 12 piu", shortDescription = "xyz +- piu", stockStatus = "onbackorder"
        )

        sut.upsertProducts(listOf(product1, product2, product3))

        val site = SiteModel().apply { id = product1.localSiteId.value }

        // Test search in name
        var products = sut.getProducts(site, emptyMap(), searchQuery = "a")
        // Products 1 and 3
        assertEquals(2, products.size)

        products = sut.getProducts(site, emptyMap(), searchQuery = "b")
        // Products 2 and 3
        assertEquals(2, products.size)

        // Product 3
        products = sut.getProducts(site, emptyMap(), searchQuery = "ab")
        assertEquals(1, products.size)

        // Test search in description
        products = sut.getProducts(site, emptyMap(), searchQuery = "1")
        // Products 1 and 3
        assertEquals(2, products.size)

        products = sut.getProducts(site, emptyMap(), searchQuery = "2")
        // Products 2 and 3
        assertEquals(2, products.size)

        products = sut.getProducts(site, emptyMap(), searchQuery = "12")
        // Product 3
        assertEquals(1, products.size)

        // Test search in short description
        products = sut.getProducts(site, emptyMap(), searchQuery = "+")
        // Products 1 and 3
        assertEquals(2, products.size)

        products = sut.getProducts(site, emptyMap(), searchQuery = "-")
        // Products 2 and 3
        assertEquals(2, products.size)

        products = sut.getProducts(site, emptyMap(), searchQuery = "+-")
        // Product 3
        assertEquals(1, products.size)

        // Test search with filter options
        products = sut.getProducts(site, mapOf(ProductFilterOption.STOCK_STATUS to "instock"),
            searchQuery = "a")
        // Product 1
        assertEquals(1, products.size)
    }


    @Test
    fun testGetProductsForSiteWithExcludedProductIds() = runTest {
        val excludedProductIds = listOf(40L)

        val product1 = ProductTestUtils.generateSampleProduct(40)
        val product2 = ProductTestUtils.generateSampleProduct(41)
        val product3 = ProductTestUtils.generateSampleProduct(42)

        sut.upsertProducts(listOf(product1, product2, product3))

        val site = SiteModel().apply { id = product1.localSiteId.value }
        val products = sut.getProducts(
                site, filterOptions = emptyMap(), excludedProductIds = excludedProductIds
        )
        assertEquals(2, products.size)
        assertEquals(41, products.first().remoteProductId)
        assertEquals(42, products.last().remoteProductId)

        // insert products with the same productId but for a different site
        val differentSiteProduct1 = ProductTestUtils.generateSampleProduct(40, siteId = 10)
        val differentSiteProduct2 = ProductTestUtils.generateSampleProduct(
            41, siteId = 10
        )
        val differentSiteProduct3 = ProductTestUtils.generateSampleProduct(
            42, siteId = 10
        )

        sut.upsertProducts(listOf(differentSiteProduct1, differentSiteProduct2, differentSiteProduct3))

        // verify that the products for the first site is still 2
        assertEquals(2, sut.getProducts(
                site, emptyMap(), excludedProductIds = excludedProductIds
        ).size)

        // verify that the products for the second site is also 2
        val site2 = SiteModel().apply { id = differentSiteProduct1.localSiteId.value }
        val differentSiteProducts = sut.getProducts(
                site2, filterOptions = emptyMap(), excludedProductIds = listOf(40, 41)
        )
        assertEquals(1, differentSiteProducts.size)
        assertEquals(42, differentSiteProducts.first().remoteProductId)
    }

    private suspend fun ProductsDao.getAllProductsForSite(siteId: Int): List<WCProductModel> {
        return observeProducts(
            siteId,
            status = null,
            stockStatus = null,
            type = null,
            category = null,
            excludeSampleProducts = false,
            limit = null,
            excludedProductIds = emptyList()
        ).first()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }
}
