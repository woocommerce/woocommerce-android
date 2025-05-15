package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
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

@RunWith(RobolectricTestRunner::class)
class ProductShippingClassesDaoTest {
    private lateinit var sut: ProductShippingClassesDao
    private lateinit var database: WCAndroidDatabase

    private val site = SiteModel().apply {
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
        sut = database.productShippingClassesDao
    }

    @Test
    fun testInsertOrUpdateProductShippingClass() = runTest {
        val shippingClass = ProductTestUtils.generateProductShippingClassList(site.id)[0]
        kotlin.test.assertNotNull(shippingClass)

        // Test inserting a product shipping class
        sut.upsertProductShippingClass(shippingClass)
        var savedShippingClassList = sut.getProductShippingClasses(site.localId())
        assertEquals(savedShippingClassList.size, 1)
        assertEquals(savedShippingClassList[0].localSiteId, shippingClass.localSiteId)
        assertEquals(savedShippingClassList[0].remoteShippingClassId, shippingClass.remoteShippingClassId)
        assertEquals(savedShippingClassList[0].name, shippingClass.name)
        assertEquals(savedShippingClassList[0].slug, shippingClass.slug)
        assertEquals(savedShippingClassList[0].description, shippingClass.description)

        // Test updating the same product shipping class
        val updatedShippingClass = shippingClass.copy(name = "Test shipping class")
        sut.upsertProductShippingClass(updatedShippingClass)

        savedShippingClassList = sut.getProductShippingClasses(site.localId())
        assertEquals(savedShippingClassList.size, 1)
        assertEquals(savedShippingClassList[0].localSiteId, updatedShippingClass.localSiteId)
        assertEquals(savedShippingClassList[0].remoteShippingClassId, updatedShippingClass.remoteShippingClassId)
        assertEquals(savedShippingClassList[0].name, updatedShippingClass.name)
        assertEquals(savedShippingClassList[0].slug, updatedShippingClass.slug)
        assertEquals(savedShippingClassList[0].description, updatedShippingClass.description)
    }

//    @Test
//    fun testInsertOrUpdateProductShippingClassList() {
//        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)
//        kotlin.test.assertTrue(shippingClassList.isNotEmpty())
//
//        // Insert product shipping class list
//        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
//        kotlin.test.assertEquals(shippingClassList.size, rowsAffected)
//    }
//
//    @Test
//    fun testGetProductShippingClassListForSite() {
//        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)
//        kotlin.test.assertTrue(shippingClassList.isNotEmpty())
//
//        // Insert product shipping class list
//        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
//        kotlin.test.assertEquals(shippingClassList.size, rowsAffected)
//
//        // Get shipping class list for site and verify
//        val savedShippingClassListExists = ProductSqlUtils.getProductShippingClassListForSite(site.id)
//        kotlin.test.assertEquals(shippingClassList.size, savedShippingClassListExists.size)
//
//        // Get shipping class list for a site that does not exist
//        val nonExistingSite = SiteModel().apply { id = 400 }
//        val savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(nonExistingSite.id)
//        kotlin.test.assertEquals(0, savedShippingClassList.size)
//    }
//
//    @Test
//    fun testGetProductShippingClassByRemoteShippingId() {
//        val shippingClass = ProductTestUtils.generateSampleProductShippingClass(
//            remoteId = 40, siteId = site.id
//        )
//
//        // Insert product shipping class list
//        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClass(shippingClass)
//        kotlin.test.assertEquals(1, rowsAffected)
//
//        // Get shipping class for site and remoteId and verify
//        val savedShippingClassExists = ProductSqlUtils.getProductShippingClassByRemoteId(
//            shippingClass.remoteShippingClassId, site.id
//        )
//        kotlin.test.assertEquals(shippingClass.remoteShippingClassId, savedShippingClassExists?.remoteShippingClassId)
//        kotlin.test.assertEquals(shippingClass.name, savedShippingClassExists?.name)
//        kotlin.test.assertEquals(shippingClass.description, savedShippingClassExists?.description)
//        kotlin.test.assertEquals(shippingClass.slug, savedShippingClassExists?.slug)
//        kotlin.test.assertEquals(shippingClass.localSiteId, savedShippingClassExists?.localSiteId)
//
//        // Get shipping class for a site that does not exist
//        val nonExistingSite = SiteModel().apply { id = 400 }
//        val savedShippingClass = ProductSqlUtils.getProductShippingClassByRemoteId(
//            25, nonExistingSite.id
//        )
//        kotlin.test.assertNull(savedShippingClass)
//
//        // Get shipping class for a site that does not exist
//        val nonExistingRemoteId = 25L
//        val nonExistentShippingClass = ProductSqlUtils.getProductShippingClassByRemoteId(
//            nonExistingRemoteId, site.id
//        )
//        kotlin.test.assertNull(nonExistentShippingClass)
//    }
//
//    @Test
//    fun testDeleteProductShippingListForSite() {
//        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)
//
//        var rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
//        kotlin.test.assertEquals(shippingClassList.size, rowsAffected)
//
//        // Verify products inserted
//        var savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
//        kotlin.test.assertEquals(shippingClassList.size, savedShippingClassList.size)
//
//        // Delete shipping class list for site and verify
//        rowsAffected = ProductSqlUtils.deleteProductShippingClassListForSite(site)
//        kotlin.test.assertEquals(shippingClassList.size, rowsAffected)
//        savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
//        kotlin.test.assertEquals(0, savedShippingClassList.size)
//    }
//
//    @Test
//    fun testDeleteSiteDeletesProductShippingClassList() {
//        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)
//        kotlin.test.assertTrue(shippingClassList.isNotEmpty())
//
//        val rowsAffected = ProductSqlUtils.insertOrUpdateProductShippingClassList(shippingClassList)
//        kotlin.test.assertEquals(shippingClassList.size, rowsAffected)
//
//        // Verify products inserted
//        var savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
//        kotlin.test.assertEquals(shippingClassList.size, savedShippingClassList.size)
//
//        // Delete site and verify shipping class list  deleted via foreign key constraint
//        TestSiteSqlUtils.siteSqlUtils.deleteSite(site)
//        savedShippingClassList = ProductSqlUtils.getProductShippingClassListForSite(site.id)
//        kotlin.test.assertEquals(0, savedShippingClassList.size)
//    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }
}
