package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.wc.product.ProductTestUtils

@RunWith(RobolectricTestRunner::class)
class ProductShippingClassesDaoTest {
    @Rule
    @JvmField
    val wcDatabaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var sut: ProductShippingClassesDao

    private val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Before
    fun setUp() {
        sut = wcDatabaseRule.db.productShippingClassesDao
    }

    @Test
    fun testInsertOrUpdateProductShippingClass() = runTest {
        val testClass = ProductTestUtils.generateProductShippingClassList(site.id)[0]

        sut.upsertProductShippingClass(testClass)

        assertThat(
            sut.getProductShippingClasses(site.localId())
        ).containsOnly(testClass)

        val updatedShippingClass = testClass.copy(name = "Test shipping class")
        sut.upsertProductShippingClass(updatedShippingClass)

        assertThat(
            sut.getProductShippingClasses(site.localId())
        ).containsOnly(updatedShippingClass)
    }

    @Test
    fun testInsertOrUpdateProductShippingClassList() = runTest {
        val testClasses = ProductTestUtils.generateProductShippingClassList(site.id)

        sut.upsertProductShippingClasses(testClasses)

        assertThat(
            sut.getProductShippingClasses(site.localId())
        ).containsExactlyInAnyOrderElementsOf(testClasses)
    }

    @Test
    fun testGetProductShippingClassListForSite() = runTest {
        val testClasses = ProductTestUtils.generateProductShippingClassList(site.id)

        sut.upsertProductShippingClasses(testClasses)

        assertThat(
            sut.getProductShippingClasses(site.localId())
        ).containsExactlyInAnyOrderElementsOf(testClasses)

        val nonExistingSiteId = LocalId(404)
        assertThat(
            sut.getProductShippingClasses(nonExistingSiteId)
        ).isEmpty()
    }

    @Test
    fun testGetProductShippingClassByRemoteShippingId() = runTest {
        val testClass = ProductTestUtils.generateSampleProductShippingClass(remoteId = 40, siteId = site.id)
        sut.upsertProductShippingClass(testClass)

        assertThat(
            sut.getProductShippingClass(site.localId(), testClass.remoteShippingClassId)
        ).isEqualTo(testClass)

        val nonExistingSiteId = LocalId(404)
        assertThat(
            sut.getProductShippingClass(nonExistingSiteId, testClass.remoteShippingClassId)
        ).isNull()

        val nonExistingProductId = RemoteId(404)
        assertThat(
            sut.getProductShippingClass(site.localId(), nonExistingProductId)
        ).isNull()
    }

    @Test
    fun testDeleteProductShippingListForSite() = runTest {
        val testClasses = ProductTestUtils.generateProductShippingClassList(site.id)
        sut.upsertProductShippingClasses(testClasses)

        sut.deleteProductShippingClasses(site.localId())

        assertThat(
            sut.getProductShippingClasses(site.localId())
        ).isEmpty()
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
}
