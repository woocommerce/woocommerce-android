package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.WCAndroidDatabase
import org.wordpress.android.fluxc.wc.product.ProductTestUtils
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class ProductCategoriesDaoTest {
    private lateinit var sut: ProductCategoriesDao
    private lateinit var database: WCAndroidDatabase

    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(context, WCAndroidDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sut = database.productCategoriesDao
    }

    @Test
    fun testInsertOrUpdateProductCategory() = runTest {
        val category = ProductTestUtils.getProductCategories(site.id)[0]
        assertNotNull(category)

        // Test inserting a product category
        sut.upsertProductCategory(category)
        var savedCategory = sut.getProductCategory(site.id, category.remoteCategoryId)

        assertNotNull(savedCategory)
        assertEquals(category.remoteCategoryId, savedCategory.remoteCategoryId)
        assertEquals(category.name, savedCategory.name)
        assertEquals(category.slug, savedCategory.slug)
        assertEquals(category.parent, savedCategory.parent)
        assertEquals(category.localSiteId, savedCategory.localSiteId)

        // Test updating the same product category
        val updated = category.copy(name = "foo")
        sut.upsertProductCategory(updated)
        savedCategory = sut.getProductCategory(site.id, category.remoteCategoryId)

        assertNotNull(savedCategory)
        assertEquals(updated.name, savedCategory.name)
    }

//    @Test
//    fun testInsertOrUpdateProductCategories() {
//        val productCategories = ProductTestUtils.getProductCategories(site.id)
//        kotlin.test.assertTrue(productCategories.isNotEmpty())
//
//        // Insert all product categories
//        val rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(productCategories)
//        kotlin.test.assertEquals(productCategories.size, rowsAffected)
//    }
//
//    @Test
//    fun testGetProductCategoriesForSite() {
//        val categories = ProductTestUtils.getProductCategories(site.id)
//        kotlin.test.assertTrue(categories.isNotEmpty())
//
//        // Insert all product categories
//        val rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(categories)
//        kotlin.test.assertEquals(categories.size, rowsAffected)
//
//        // Get all product categories for site and verify
//        val savedCategoriesExist = ProductSqlUtils.getProductCategoriesForSite(site)
//        kotlin.test.assertEquals(categories.size, savedCategoriesExist.size)
//
//        // Get all product categories for a site that do not exist
//        val savedCategories = ProductSqlUtils.getProductCategoriesForSite(SiteModel().apply { id = 400 })
//        kotlin.test.assertEquals(0, savedCategories.size)
//    }
//
//    @Test
//    fun testDeleteAllProductCategories() {
//        val categories = ProductTestUtils.getProductCategories(site.id)
//        kotlin.test.assertTrue(categories.isNotEmpty())
//        var rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(categories)
//        kotlin.test.assertEquals(categories.size, rowsAffected)
//
//        // Verify categories inserted
//        var savedCategories = ProductSqlUtils.getProductCategoriesForSite(site)
//        kotlin.test.assertEquals(categories.size, savedCategories.size)
//
//        // Delete all categories and verify
//        rowsAffected = ProductSqlUtils.deleteAllProductCategories()
//        kotlin.test.assertEquals(categories.size, rowsAffected)
//        savedCategories = ProductSqlUtils.getProductCategoriesForSite(site)
//        kotlin.test.assertEquals(0, savedCategories.size)
//    }
//
//    @Test
//    fun testDeleteProductCategoriesForSite() {
//        val categories = ProductTestUtils.getProductCategories(site.id)
//
//        var rowsAffected = ProductSqlUtils.insertOrUpdateProductCategories(categories)
//        kotlin.test.assertEquals(categories.size, rowsAffected)
//
//        // Verify categories inserted
//        var savedCategories = ProductSqlUtils.getProductCategoriesForSite(site)
//        kotlin.test.assertEquals(categories.size, savedCategories.size)
//
//        // Delete categories for site and verify
//        rowsAffected = ProductSqlUtils.deleteAllProductCategoriesForSite(site)
//        kotlin.test.assertEquals(categories.size, rowsAffected)
//        savedCategories = ProductSqlUtils.getProductCategoriesForSite(site)
//        kotlin.test.assertEquals(0, savedCategories.size)
//    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

}
