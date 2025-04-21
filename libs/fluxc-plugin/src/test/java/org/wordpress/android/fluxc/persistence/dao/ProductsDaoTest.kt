package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.ProductSqlUtils
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.wc.product.ProductTestUtils
import kotlin.test.assertEquals

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
        val storedProductsCount = sut.getProducts(site.id, listOf(productModel.remoteProductId)).count()
        assertEquals(1, storedProductsCount)

        // Test updating product
        val storedProduct = sut.getProduct(site.id, productModel.remoteProductId)?.copy(
            name = "Anitaa Test",
            virtual = true
        )
        storedProduct?.also {
            sut.upsertProduct(it)
        }

        val updatedProductsCount = sut.getProducts(site.id, listOf(productModel.remoteProductId)).count()
        assertEquals(1, updatedProductsCount)

        val updatedProduct = sut.getProduct(site.id, productModel.remoteProductId)
        assertEquals(storedProduct?.remoteProductId, updatedProduct?.remoteProductId)
        assertEquals(storedProduct?.name, updatedProduct?.name)
        assertEquals(storedProduct?.virtual, updatedProduct?.virtual)
    }
}
