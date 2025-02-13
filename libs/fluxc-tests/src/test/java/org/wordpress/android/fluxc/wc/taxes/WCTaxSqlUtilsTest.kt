package org.wordpress.android.fluxc.wc.taxes

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.TestSiteSqlUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.taxes.WCTaxClassModel
import org.wordpress.android.fluxc.persistence.WCTaxSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCTaxSqlUtilsTest {
    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(SiteModel::class.java, WCTaxClassModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()

        // Insert the site into the db so it's available later for tax classes
        TestSiteSqlUtils.siteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun testInsertOrUpdateTaxClassForSite() {
        val taxClass = TaxTestUtils.generateTaxClassList(site.id)[0]
        assertNotNull(taxClass)

        // Test inserting a tax class
        var rowsAffected = WCTaxSqlUtils.insertOrUpdateTaxClass(taxClass)
        assertEquals(1, rowsAffected)
        var savedTaxClassList = WCTaxSqlUtils.getTaxClassesForSite(site.id)
        assertEquals(savedTaxClassList.size, 1)
        assertEquals(savedTaxClassList[0].localSiteId, taxClass.localSiteId)
        assertEquals(savedTaxClassList[0].name, taxClass.name)
        assertEquals(savedTaxClassList[0].slug, taxClass.slug)

        // Test updating the same product shipping class
        taxClass.apply {
            name = "Test tax class"
        }
        rowsAffected = WCTaxSqlUtils.insertOrUpdateTaxClass(taxClass)
        assertEquals(1, rowsAffected)
        savedTaxClassList = WCTaxSqlUtils.getTaxClassesForSite(site.id)
        assertEquals(savedTaxClassList.size, 1)
        assertEquals(savedTaxClassList[0].localSiteId, taxClass.localSiteId)
        assertEquals(savedTaxClassList[0].name, taxClass.name)
        assertEquals(savedTaxClassList[0].slug, taxClass.slug)
    }

    @Test
    fun testInsertOrUpdateProductTaxClassList() {
        val taxClassList = TaxTestUtils.generateTaxClassList(site.id)
        assertTrue(taxClassList.isNotEmpty())

        // Insert product shipping class list
        val rowsAffected = WCTaxSqlUtils.insertOrUpdateTaxClasses(taxClassList)
        assertEquals(taxClassList.size, rowsAffected)
    }

    @Test
    fun testGetTaxClassListForSite() {
        val taxClassList = TaxTestUtils.generateTaxClassList(site.id)
        assertTrue(taxClassList.isNotEmpty())

        // Insert product shipping class list
        val rowsAffected = WCTaxSqlUtils.insertOrUpdateTaxClasses(taxClassList)
        assertEquals(taxClassList.size, rowsAffected)

        // Get tax class list for site and verify
        val savedTaxClassListExists = WCTaxSqlUtils.getTaxClassesForSite(site.id)
        assertEquals(taxClassList.size, savedTaxClassListExists.size)

        // Get tax class list for a site that does not exist
        val nonExistingSite = SiteModel().apply { id = 400 }
        val savedTaxClassList = WCTaxSqlUtils.getTaxClassesForSite(nonExistingSite.id)
        assertEquals(0, savedTaxClassList.size)
    }

    @Test
    fun testDeleteTaxClassListForSite() {
        val taxClassList = TaxTestUtils.generateTaxClassList(site.id)

        var rowsAffected = WCTaxSqlUtils.insertOrUpdateTaxClasses(taxClassList)
        assertEquals(taxClassList.size, rowsAffected)

        // Verify tax class list inserted
        var savedTaxClassList = WCTaxSqlUtils.getTaxClassesForSite(site.id)
        assertEquals(taxClassList.size, savedTaxClassList.size)

        // Delete tax class list for site and verify
        rowsAffected = WCTaxSqlUtils.deleteTaxClassesForSite(site)
        assertEquals(taxClassList.size, rowsAffected)
        savedTaxClassList = WCTaxSqlUtils.getTaxClassesForSite(site.id)
        assertEquals(0, savedTaxClassList.size)
    }

    @Test
    fun testDeleteSiteDeletesProductShippingClassList() {
        val taxClassList = TaxTestUtils.generateTaxClassList(site.id)
        assertTrue(taxClassList.isNotEmpty())

        val rowsAffected = WCTaxSqlUtils.insertOrUpdateTaxClasses(taxClassList)
        assertEquals(taxClassList.size, rowsAffected)

        // Verify tax class list inserted
        var savedTaxClassList = WCTaxSqlUtils.getTaxClassesForSite(site.id)
        assertEquals(taxClassList.size, savedTaxClassList.size)

        // Delete site and verify tac class list  deleted via foreign key constraint
        TestSiteSqlUtils.siteSqlUtils.deleteSite(site)
        savedTaxClassList = WCTaxSqlUtils.getTaxClassesForSite(site.id)
        assertEquals(0, savedTaxClassList.size)
    }
}
