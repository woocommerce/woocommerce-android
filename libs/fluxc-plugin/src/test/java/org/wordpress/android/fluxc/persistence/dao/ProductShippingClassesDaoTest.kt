package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
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

    @Test
    fun testInsertOrUpdateProductShippingClassList() = runTest {
        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)
        assertTrue(shippingClassList.isNotEmpty())

        // Insert product shipping class list
        sut.upsertProductShippingClasses(shippingClassList)
        assertThat(sut.getProductShippingClasses(site.localId())).containsExactlyInAnyOrderElementsOf(shippingClassList)
    }

    @Test
    fun testGetProductShippingClassListForSite() = runTest {
        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)
        assertTrue(shippingClassList.isNotEmpty())

        // Insert product shipping class list
        sut.upsertProductShippingClasses(shippingClassList)

        // Get shipping class list for site and verify
        val savedShippingClassListExists = sut.getProductShippingClasses(site.localId())
        assertEquals(shippingClassList.size, savedShippingClassListExists.size)

        // Get shipping class list for a site that does not exist
        val nonExistingSite = SiteModel().apply { id = 400 }
        val savedShippingClassList = sut.getProductShippingClasses(nonExistingSite.localId())
        assertEquals(0, savedShippingClassList.size)
    }

    @Test
    fun testGetProductShippingClassByRemoteShippingId() = runTest {
        val shippingClass = ProductTestUtils.generateSampleProductShippingClass(
            remoteId = 40, siteId = site.id
        )

        // Insert product shipping class list
        sut.upsertProductShippingClass(shippingClass)

        // Get shipping class for site and remoteId and verify
        val savedShippingClassExists = sut.getProductShippingClass(
            site.localId(), shippingClass.remoteShippingClassId,
        )
        assertEquals(shippingClass.remoteShippingClassId, savedShippingClassExists?.remoteShippingClassId)
        assertEquals(shippingClass.name, savedShippingClassExists?.name)
        assertEquals(shippingClass.description, savedShippingClassExists?.description)
        assertEquals(shippingClass.slug, savedShippingClassExists?.slug)
        assertEquals(shippingClass.localSiteId, savedShippingClassExists?.localSiteId)

        // Get shipping class for a site that does not exist
        val nonExistingSite = SiteModel().apply { id = 400 }
        val savedShippingClass = sut.getProductShippingClass(
            nonExistingSite.localId(), RemoteId(25)
        )
        kotlin.test.assertNull(savedShippingClass)

        // Get shipping class for a site that does not exist
        val nonExistingRemoteId = RemoteId(25L)
        val nonExistentShippingClass = sut.getProductShippingClass(
            site.localId(), nonExistingRemoteId
        )
        assertNull(nonExistentShippingClass)
    }

    @Test
    fun testDeleteProductShippingListForSite() = runTest {
        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)

        sut.upsertProductShippingClasses(shippingClassList)

        // Verify products inserted
        var savedShippingClassList = sut.getProductShippingClasses(site.localId())
        assertEquals(shippingClassList.size, savedShippingClassList.size)

        // Delete shipping class list for site and verify
        sut.deleteProductShippingClasses(site.localId())
        savedShippingClassList = sut.getProductShippingClasses(site.localId())
        assertEquals(0, savedShippingClassList.size)
    }

    @Test
    @Ignore("This test is ignored until SiteModel is moved to Room and foreign key constraints are added")
    fun testDeleteSiteDeletesProductShippingClassList() = runTest {
        val shippingClassList = ProductTestUtils.generateProductShippingClassList(site.id)
        assertTrue(shippingClassList.isNotEmpty())

        sut.upsertProductShippingClasses(shippingClassList)

        // Verify products inserted
        var savedShippingClassList = sut.getProductShippingClasses(site.localId())
        assertEquals(shippingClassList.size, savedShippingClassList.size)

        // Delete site and verify shipping class list  deleted via foreign key constraint
        SiteSqlUtils().deleteSite(site)
        savedShippingClassList = sut.getProductShippingClasses(site.localId())
        assertEquals(0, savedShippingClassList.size)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }
}
