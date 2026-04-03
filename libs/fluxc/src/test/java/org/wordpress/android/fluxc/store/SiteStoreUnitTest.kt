package org.wordpress.android.fluxc.store

import androidx.test.core.app.ApplicationProvider
import com.wellsql.generated.SiteModelTable
import com.yarolegovich.wellsql.WellSql
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.persistence.AccountMapper
import org.wordpress.android.fluxc.persistence.AccountStorePersistence
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.SiteStorePersistence
import org.wordpress.android.fluxc.persistence.SiteStorePersistence.DuplicateSiteException
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.site.SiteUtils
import org.wordpress.android.fluxc.store.SiteStore.UpdateSitesResult
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.lang.reflect.InvocationTargetException

@Suppress("UnitTestNamingRule")
@RunWith(RobolectricTestRunner::class)
class SiteStoreUnitTest {
    @Rule
    @JvmField
    var wpDatabaseRule: WPDatabaseTestRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    private val mSiteSqlUtils = SiteSqlUtils()
    private lateinit var mSiteStorePersistence: SiteStorePersistence
    private lateinit var mSiteStore: SiteStore

    @Before
    fun setUp() {
        val config = WellSqlConfig(ApplicationProvider.getApplicationContext())
        WellSql.init(config)
        config.reset()

        val accountStorePersistence = AccountStorePersistence(
            wpDatabaseRule.db,
            AccountMapper()
        )
        val account = AccountModel()
        account.userId = 20151021
        accountStorePersistence.insertOrUpdateDefaultAccount(account)

        mSiteStorePersistence = SiteStorePersistence(accountStorePersistence)
        mSiteStore = SiteStore(
            Dispatcher(),
            mock(),
            mock(),
            mock(),
            mSiteSqlUtils,
            SiteStorePersistence(accountStorePersistence),
            mock(),
            initCoroutineEngine()
        )
    }

    @Test
    fun testSimpleInsertionAndRetrieval() {
        val siteModel = SiteModel()
        siteModel.siteId = 42
        WellSql.insert(siteModel).execute()

        assertEquals(1, mSiteStore.sites.size.toLong())

        assertEquals(42, mSiteStore.sites[0].siteId)
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertOrUpdateSite() {
        val site = SiteUtils.generateWPComSite()
        mSiteStorePersistence.insertOrUpdateSite(site)

        assertTrue(mSiteStore.hasSiteWithLocalId(site.id))
        assertEquals(site.siteId, mSiteStore.getSiteByLocalId(site.id)?.siteId)
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testSelfHostedAndJetpackSites() {
        // Note: not using the helper methods to make sure of the SiteModel definition
        val ponySite = SiteModel()
        ponySite.xmlRpcUrl = "http://pony.com/xmlrpc.php"
        ponySite.siteId = 1
        ponySite.setIsWPCom(false)
        ponySite.origin = SiteModel.ORIGIN_XMLRPC
        mSiteStorePersistence.insertOrUpdateSite(ponySite)

        val jetpackOverXMLRPC = SiteModel()
        jetpackOverXMLRPC.xmlRpcUrl = "http://pony2.com/xmlrpc.php"
        jetpackOverXMLRPC.siteId = 2
        jetpackOverXMLRPC.setIsWPCom(false)
        jetpackOverXMLRPC.setIsJetpackInstalled(true)
        jetpackOverXMLRPC.setIsJetpackConnected(true)
        jetpackOverXMLRPC.origin = SiteModel.ORIGIN_XMLRPC
        mSiteStorePersistence.insertOrUpdateSite(jetpackOverXMLRPC)

        val jetpackOverRest = SiteModel()
        jetpackOverRest.xmlRpcUrl = "http://pony3.com/xmlrpc.php"
        jetpackOverRest.siteId = 3
        jetpackOverRest.setIsWPCom(false)
        jetpackOverRest.setIsJetpackInstalled(true)
        jetpackOverRest.setIsJetpackConnected(true)
        jetpackOverRest.origin = SiteModel.ORIGIN_WPCOM_REST
        mSiteStorePersistence.insertOrUpdateSite(jetpackOverRest)

        assertEquals(3, mSiteStore.sites.size.toLong())

        // User "install and connect" ponySite site to Jetpack via his connected .com account
        ponySite.setIsJetpackInstalled(true)
        ponySite.setIsJetpackConnected(true)
        ponySite.origin = SiteModel.ORIGIN_WPCOM_REST
        mSiteStorePersistence.insertOrUpdateSite(ponySite)

        assertEquals(3, mSiteStore.sites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testWPComSiteVisibility() {
        // Should not cause any errors
        mSiteStore.isWPComSiteVisibleByLocalId(45)
        mSiteSqlUtils.setSiteVisibility(null, true)

        val selfHostedNonJPSite = SiteUtils.generateSelfHostedNonJPSite()
        mSiteStorePersistence.insertOrUpdateSite(selfHostedNonJPSite)

        // Attempt to use with id of self-hosted site
        mSiteSqlUtils.setSiteVisibility(selfHostedNonJPSite, false)
        // The self-hosted site should not be affected
        assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.id)?.isVisible == true)

        val wpComSite = SiteUtils.generateWPComSite()
        mSiteStorePersistence.insertOrUpdateSite(wpComSite)

        // Attempt to use with legitimate .com site
        mSiteSqlUtils.setSiteVisibility(selfHostedNonJPSite, false)
        assertFalse(mSiteStore.getSiteByLocalId(wpComSite.id)?.isVisible == true)
        assertFalse(mSiteStore.isWPComSiteVisibleByLocalId(wpComSite.id))
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testSetAllWPComSitesVisibility() {
        val selfHostedNonJPSite = SiteUtils.generateSelfHostedNonJPSite()
        mSiteStorePersistence.insertOrUpdateSite(selfHostedNonJPSite)

        // Attempt to use with id of self-hosted site
        for (site in mSiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel()) {
            mSiteSqlUtils.setSiteVisibility(site, false)
        }
        // The self-hosted site should not be affected
        assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.id)?.isVisible == true)

        val wpComSite1 = SiteUtils.generateWPComSite()
        val wpComSite2 = SiteUtils.generateWPComSite()
        wpComSite2.id = 44
        wpComSite2.siteId = 284

        mSiteStorePersistence.insertOrUpdateSite(wpComSite1)
        mSiteStorePersistence.insertOrUpdateSite(wpComSite2)

        // Attempt to use with legitimate .com site
        for (site in mSiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel()) {
            mSiteSqlUtils.setSiteVisibility(site, false)
        }
        assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.id)?.isVisible == true)
        assertFalse(mSiteStore.getSiteByLocalId(wpComSite1.id)?.isVisible == true)
        assertFalse(mSiteStore.getSiteByLocalId(wpComSite2.id)?.isVisible == true)
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testGetIdForIdMethods() {
        assertEquals(0, mSiteStore.getLocalIdForRemoteSiteId(555).toLong())
        assertEquals(0, mSiteStore.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(2626, "").toLong())
        assertEquals(0, mSiteStore.getSiteIdForLocalId(5577))

        val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()
        val wpComSite = SiteUtils.generateWPComSite()
        val jetpackSite = SiteUtils.generateJetpackSiteOverXMLRPC()
        mSiteStorePersistence.insertOrUpdateSite(selfHostedSite)
        mSiteStorePersistence.insertOrUpdateSite(wpComSite)
        mSiteStorePersistence.insertOrUpdateSite(jetpackSite)

        assertEquals(
            selfHostedSite.id.toLong(),
            mSiteStore.getLocalIdForRemoteSiteId(selfHostedSite.selfHostedSiteId).toLong()
        )
        assertEquals(
            wpComSite.id.toLong(),
            mSiteStore.getLocalIdForRemoteSiteId(wpComSite.siteId).toLong()
        )

        // Should be able to look up a Jetpack site by .com and by .org id (assuming it's been set)
        assertEquals(
            jetpackSite.id.toLong(),
            mSiteStore.getLocalIdForRemoteSiteId(jetpackSite.siteId).toLong()
        )
        assertEquals(
            jetpackSite.id.toLong(),
            mSiteStore.getLocalIdForRemoteSiteId(jetpackSite.selfHostedSiteId).toLong()
        )

        assertEquals(
            selfHostedSite.id.toLong(), mSiteStore.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(
                selfHostedSite.selfHostedSiteId, selfHostedSite.xmlRpcUrl
            ).toLong()
        )
        assertEquals(
            jetpackSite.id.toLong(), mSiteStore.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(
                jetpackSite.selfHostedSiteId, jetpackSite.xmlRpcUrl
            ).toLong()
        )

        assertEquals(
            selfHostedSite.selfHostedSiteId,
            mSiteStore.getSiteIdForLocalId(selfHostedSite.id)
        )
        assertEquals(wpComSite.siteId, mSiteStore.getSiteIdForLocalId(wpComSite.id))
        assertEquals(jetpackSite.siteId, mSiteStore.getSiteIdForLocalId(jetpackSite.id))
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testGetSiteBySiteId() {
        assertNull(mSiteStore.getSiteBySiteId(555))

        val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()
        val wpComSite = SiteUtils.generateWPComSite()
        val jetpackSiteOverXMLRPC = SiteUtils.generateJetpackSiteOverXMLRPC()
        mSiteStorePersistence.insertOrUpdateSite(selfHostedSite)
        mSiteStorePersistence.insertOrUpdateSite(wpComSite)
        mSiteStorePersistence.insertOrUpdateSite(jetpackSiteOverXMLRPC)

        assertEquals(1, mSiteSqlUtils.sitesAccessedViaWPComRest.getAsCursor().count.toLong())
        assertNotNull(mSiteStore.getSiteBySiteId(wpComSite.siteId))
        assertNotNull(mSiteStore.getSiteBySiteId(jetpackSiteOverXMLRPC.siteId))
        assertNull(mSiteStore.getSiteBySiteId(selfHostedSite.siteId))
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testDeleteSite() {
        val wpComSite = SiteUtils.generateWPComSite()

        // Should not cause any errors
        mSiteSqlUtils.deleteSite(wpComSite)

        mSiteStorePersistence.insertOrUpdateSite(wpComSite)
        val affectedRows = mSiteSqlUtils.deleteSite(wpComSite)

        assertEquals(1, affectedRows.toLong())
        assertEquals(0, mSiteStore.sites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testGetWPComSites() {
        val wpComSite = SiteUtils.generateWPComSite()
        val jetpackSiteOverXMLRPC = SiteUtils.generateJetpackSiteOverXMLRPC()
        val jetpackSiteOverRestOnly = SiteUtils.generateJetpackSiteOverRestOnly()

        mSiteStorePersistence.insertOrUpdateSite(wpComSite)
        mSiteStorePersistence.insertOrUpdateSite(jetpackSiteOverXMLRPC)
        mSiteStorePersistence.insertOrUpdateSite(jetpackSiteOverRestOnly)

        assertEquals(2, mSiteSqlUtils.sitesAccessedViaWPComRest.getAsCursor().count.toLong())

        val wpComSites = mSiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel()
        assertEquals(1, wpComSites.size.toLong())
        for (site in wpComSites) {
            assertNotEquals(jetpackSiteOverXMLRPC.id.toLong(), site.id.toLong())
        }
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertDuplicateSites() {
        val futureJetpack = SiteUtils.generateSelfHostedSiteFutureJetpack()
        val jetpack = SiteUtils.generateJetpackSiteOverRestOnly()

        // Insert a self hosted site that will later be converted to Jetpack
        mSiteStorePersistence.insertOrUpdateSite(futureJetpack)

        // Insert the same site but Jetpack powered this time
        mSiteStorePersistence.insertOrUpdateSite(jetpack)

        // Previous site should be converted to a Jetpack site and we should see only one site
        val sitesCount = WellSql.select(SiteModel::class.java).getAsCursor().count
        assertEquals(1, sitesCount.toLong())

        val wpComSites = mSiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel()
        assertEquals(0, wpComSites.size.toLong())
        assertEquals(1, mSiteSqlUtils.sitesAccessedViaWPComRest.getAsCursor().count.toLong())
        val jetpackSites =
            mSiteSqlUtils.getSitesWith(SiteModelTable.IS_JETPACK_CONNECTED, true).getAsModel()
        assertEquals(jetpack.siteId, jetpackSites[0]?.siteId)
        assertTrue(jetpackSites[0].isJetpackConnected)
        assertFalse(jetpackSites[0].isWPCom)
    }

    @Test
    @Suppress("SwallowedException", "unused")
    @Throws(DuplicateSiteException::class)
    fun testInsertDuplicateSitesError() {
        val futureJetpack = SiteUtils.generateSelfHostedSiteFutureJetpack()
        val jetpack = SiteUtils.generateJetpackSiteOverRestOnly()

        // Insert a Jetpack powered site
        mSiteStorePersistence.insertOrUpdateSite(jetpack)
        var duplicate = false
        try {
            // Insert the same site but via self hosted this time (this should fail)
            mSiteStorePersistence.insertOrUpdateSite(futureJetpack)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        assertTrue(duplicate)
        val sitesCount = WellSql.select(SiteModel::class.java).getAsCursor().count
        assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Suppress("SwallowedException", "unused")
    @Throws(DuplicateSiteException::class)
    fun testInsertDuplicateSitesDifferentSchemesError1() {
        val futureJetpack = SiteUtils.generateSelfHostedSiteFutureJetpack()
        val jetpack = SiteUtils.generateJetpackSiteOverRestOnly()

        futureJetpack.xmlRpcUrl = "https://pony.com/xmlrpc.php"
        jetpack.xmlRpcUrl = "http://pony.com/xmlrpc.php"

        // Insert a Jetpack powered site
        mSiteStorePersistence.insertOrUpdateSite(jetpack)
        var duplicate = false
        try {
            // Insert the same site but via self hosted this time (this should fail)
            mSiteStorePersistence.insertOrUpdateSite(futureJetpack)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        assertTrue(duplicate)
        val sitesCount = WellSql.select(SiteModel::class.java).getAsCursor().count
        assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Suppress("SwallowedException", "unused")
    @Throws(DuplicateSiteException::class)
    fun testInsertDuplicateSitesDifferentSchemesError2() {
        val futureJetpack = SiteUtils.generateSelfHostedSiteFutureJetpack()
        val jetpack = SiteUtils.generateJetpackSiteOverRestOnly()

        futureJetpack.xmlRpcUrl = "http://pony.com/xmlrpc.php"
        jetpack.xmlRpcUrl = "https://pony.com/xmlrpc.php"

        // Insert a Jetpack powered site
        mSiteStorePersistence.insertOrUpdateSite(jetpack)
        var duplicate = false
        try {
            // Insert the same site but via self hosted this time (this should fail)
            mSiteStorePersistence.insertOrUpdateSite(futureJetpack)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        assertTrue(duplicate)
        val sitesCount = WellSql.select(SiteModel::class.java).getAsCursor().count
        assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Suppress("SwallowedException", "unused")
    @Throws(DuplicateSiteException::class)
    fun testInsertDuplicateXmlRpcJetpackSite() {
        val jetpackXmlRpcSite = SiteUtils.generateJetpackSiteOverXMLRPC()

        jetpackXmlRpcSite.url = "http://some.url"

        // Insert a Jetpack powered site over XML-RPC
        mSiteStorePersistence.insertOrUpdateSite(jetpackXmlRpcSite)

        // Set up the same site (by URL/XML-RPC URL), but don't identify it as a Jetpack site
        // This simulates sites resulting from wp.getUsersBlogs, which don't have the site ID and can't be identified
        // as Jetpack or not (wp.getOptions is the call that returns that information)
        val jetpackXmlRpcSite2 = SiteUtils.generateSelfHostedNonJPSite()
        jetpackXmlRpcSite2.xmlRpcUrl = jetpackXmlRpcSite.xmlRpcUrl
        jetpackXmlRpcSite2.url = jetpackXmlRpcSite.url
        jetpackXmlRpcSite2.selfHostedSiteId = jetpackXmlRpcSite.selfHostedSiteId
        jetpackXmlRpcSite2.username = jetpackXmlRpcSite.username
        jetpackXmlRpcSite2.password = jetpackXmlRpcSite.password

        var duplicate = false
        try {
            // Insert the same site but not identified as a Jetpack site
            // (this should succeed, replacing the existing site, because the site replaced is not using the REST API)
            mSiteStorePersistence.insertOrUpdateSite(jetpackXmlRpcSite2)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        assertFalse(duplicate)
        val sitesCount = WellSql.select(SiteModel::class.java).getAsCursor().count
        assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testSearchSitesByNameMatching() {
        val wpComSite1 = SiteUtils.generateWPComSite()
        wpComSite1.name = "Doctor Emmet Brown Homepage"
        val wpComSite2 = SiteUtils.generateWPComSite()
        wpComSite2.name = "Shield Eyes from light"
        wpComSite2.siteId = 557
        val wpComSite3 = SiteUtils.generateWPComSite()
        wpComSite3.name = "I remember when this was all farmland as far as the eye could see"
        wpComSite2.siteId = 558

        mSiteStorePersistence.insertOrUpdateSite(wpComSite1)
        mSiteStorePersistence.insertOrUpdateSite(wpComSite2)
        mSiteStorePersistence.insertOrUpdateSite(wpComSite3)

        var matchingSites: List<SiteModel> = mSiteSqlUtils.getSitesByNameOrUrlMatching("eye")
        assertEquals(2, matchingSites.size.toLong())

        matchingSites = mSiteSqlUtils.getSitesByNameOrUrlMatching("EYE")
        assertEquals(2, matchingSites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testSearchSitesByNameOrUrlMatching() {
        val wpComSite1 = SiteUtils.generateWPComSite()
        wpComSite1.name = "Doctor Emmet Brown Homepage"
        val wpComSite2 = SiteUtils.generateWPComSite()
        wpComSite2.url = "shieldeyesfromlight.wordpress.com"
        val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()
        selfHostedSite.name = "I remember when this was all farmland as far as the eye could see."

        mSiteStorePersistence.insertOrUpdateSite(wpComSite1)
        mSiteStorePersistence.insertOrUpdateSite(wpComSite2)
        mSiteStorePersistence.insertOrUpdateSite(selfHostedSite)

        var matchingSites: List<SiteModel> = mSiteSqlUtils.getSitesByNameOrUrlMatching("eye")
        assertEquals(2, matchingSites.size.toLong())

        matchingSites = mSiteSqlUtils.getSitesByNameOrUrlMatching("EYE")
        assertEquals(2, matchingSites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testSearchWPComSitesByNameOrUrlMatching() {
        val wpComSite1 = SiteUtils.generateWPComSite()
        wpComSite1.name = "Doctor Emmet Brown Homepage"
        val wpComSite2 = SiteUtils.generateWPComSite()
        wpComSite2.url = "shieldeyesfromlight.wordpress.com"
        val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()
        selfHostedSite.name = "I remember when this was all farmland as far as the eye could see."

        mSiteStorePersistence.insertOrUpdateSite(wpComSite1)
        mSiteStorePersistence.insertOrUpdateSite(wpComSite2)
        mSiteStorePersistence.insertOrUpdateSite(selfHostedSite)

        var matchingSites: List<SiteModel?> = mSiteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching("eye")
        assertEquals(1, matchingSites.size.toLong())

        matchingSites = mSiteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching("EYE")
        assertEquals(1, matchingSites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testRemoveAllSites() {
        val wpComSite = SiteUtils.generateWPComSite()
        val jetpackXMLRPCSite = SiteUtils.generateJetpackSiteOverXMLRPC()
        val jetpackRestSite = SiteUtils.generateJetpackSiteOverRestOnly()
        val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()

        mSiteStorePersistence.insertOrUpdateSite(wpComSite)
        mSiteStorePersistence.insertOrUpdateSite(jetpackXMLRPCSite)
        mSiteStorePersistence.insertOrUpdateSite(jetpackRestSite)
        mSiteStorePersistence.insertOrUpdateSite(selfHostedSite)

        // first make sure sites are inserted successfully
        assertEquals(4, mSiteStore.sites.size.toLong())

        mSiteSqlUtils.deleteAllSites()

        assertEquals(0, mSiteStore.sites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testWPComAutomatedTransfer() {
        val wpComSite = SiteUtils.generateWPComSite()
        mSiteStorePersistence.insertOrUpdateSite(wpComSite)

        // Turn WP.com site into an Automated Transfer (Jetpack) site
        val automatedTransferSite = SiteUtils.generateWPComSite()
        automatedTransferSite.setIsJetpackInstalled(true)
        automatedTransferSite.setIsJetpackConnected(true)
        automatedTransferSite.setIsWPCom(false)
        automatedTransferSite.setIsAutomatedTransfer(true)

        mSiteStorePersistence.insertOrUpdateSite(automatedTransferSite)

        assertEquals(1, mSiteStore.sites.size.toLong())
    }

    @Test
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    fun testBatchInsertSiteNoDuplicateWPCom() {
        val siteList: MutableList<SiteModel?> = ArrayList()
        siteList.add(SiteUtils.generateTestSite(1, "https://pony1.com", "https://pony1.com/xmlrpc.php", true, true))
        siteList.add(SiteUtils.generateTestSite(2, "https://pony2.com", "https://pony2.com/xmlrpc.php", true, true))
        siteList.add(SiteUtils.generateTestSite(3, "https://pony3.com", "https://pony3.com/xmlrpc.php", true, true))
        siteList.add(SiteUtils.generateTestSite(4, "https://pony4.com", "https://pony4.com/xmlrpc.php", true, true))
        siteList.add(SiteUtils.generateTestSite(5, "https://pony5.com", "https://pony5.com/xmlrpc.php", true, true))

        val sites = SitesModel(siteList)

        // Use reflection to call a private Store method: equivalent to mSiteStore.updateSites(sites)
        val createOrUpdateSites = SiteStore::class.java.getDeclaredMethod("createOrUpdateSites", SitesModel::class.java)
        createOrUpdateSites.isAccessible = true
        val res = createOrUpdateSites.invoke(mSiteStore, sites) as UpdateSitesResult?

        assertFalse(res!!.duplicateSiteFound)
        assertEquals(5, res.rowsAffected.toLong())
        assertEquals(5, mSiteStore.sites.size.toLong())
    }

    @Test
    @Suppress("SwallowedException", "unused")
    @Throws(DuplicateSiteException::class)
    fun testInsertSiteDuplicateXmlRpcTrailingSlash() {
        // It's possible for the URL in `wp.getOptions` to be different from the URL in `wp.getUsersBlogs`,
        // sometimes just by a trailing slash
        // This test checks that we can still identify two sites as being identical in this case, and that we quietly
        // update the existing site rather than throw a duplicate site exception
        val selfhostedSite = SiteUtils.generateSelfHostedNonJPSite()
        selfhostedSite.url = "http://some.url"

        mSiteStorePersistence.insertOrUpdateSite(selfhostedSite)

        val selfhostedSite2 = SiteUtils.generateSelfHostedNonJPSite()
        selfhostedSite2.url = "http://some.url/"

        var duplicate = false
        try {
            // Insert the same site with a trailing slash (this should succeed, replacing the existing site)
            mSiteStorePersistence.insertOrUpdateSite(selfhostedSite2)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        assertFalse(duplicate)
        val sitesCount = WellSql.select(SiteModel::class.java).getAsCursor().count
        assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Suppress("SwallowedException", "unused")
    @Throws(DuplicateSiteException::class)
    fun testInsertSiteDuplicateXmlRpcDifferentUrl() {
        // It's possible for the URL in `wp.getOptions` to be different from the URL in `wp.getUsersBlogs`
        // This test checks that we can still identify two sites as being identical in this case, and that we quietly
        // update the existing site rather than throw a duplicate site exception
        val selfhostedSite = SiteUtils.generateSelfHostedNonJPSite()
        selfhostedSite.url = "http://some.url"
        selfhostedSite.xmlRpcUrl = "http://some.url/xmlrpc.php"

        mSiteStorePersistence.insertOrUpdateSite(selfhostedSite)

        val selfhostedSite2 = SiteUtils.generateSelfHostedNonJPSite()
        selfhostedSite2.url = "http://user5242.stagingsite.url"
        selfhostedSite2.xmlRpcUrl = "http://some.url/xmlrpc.php"

        var duplicate = false
        try {
            // Insert the same site with a different URL, but the same XML-RPC URL
            // (this should succeed, replacing the existing site)
            mSiteStorePersistence.insertOrUpdateSite(selfhostedSite2)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        assertFalse(duplicate)
        val sitesCount = WellSql.select(SiteModel::class.java).getAsCursor().count
        assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Suppress("SwallowedException", "unused")
    @Throws(DuplicateSiteException::class)
    fun testUpdateSiteUniqueConstraintFail() {
        // Create 2 test sites
        val site1 = SiteUtils.generateTestSite(0, "https://pony1.com", "https://pony1.com/xmlrpc.php", false, true)
        mSiteStorePersistence.insertOrUpdateSite(site1)
        val site2 = SiteUtils.generateTestSite(0, "https://pony2.com", "https://pony2.com/xmlrpc.php", false, true)
        mSiteStorePersistence.insertOrUpdateSite(site2)

        // Update the second site and reuse the site url and id from the first
        site2.url = "https://pony1.com"
        var duplicate = false
        try {
            mSiteStorePersistence.insertOrUpdateSite(site2)
        } catch (e: DuplicateSiteException) {
            duplicate = true
        }
        assertTrue(duplicate)
    }

    @Test
    fun testJetpackSelfHostedAndForceXMLRPC() {
        val jetpackSite = SiteUtils.generateJetpackSiteOverXMLRPC()
        jetpackSite.origin = SiteModel.ORIGIN_WPCOM_REST
        assertTrue(jetpackSite.isUsingWpComRestApi)

        // Force the origin, it should now use XMLRPC instead of REST.
        jetpackSite.origin = SiteModel.ORIGIN_XMLRPC
        assertFalse(jetpackSite.isUsingWpComRestApi)
    }

    @Test
    fun testDefaultUsageWpComRestApi() {
        val wpComSite = SiteUtils.generateWPComSite()
        assertTrue(wpComSite.isUsingWpComRestApi)

        val jetpack1 = SiteUtils.generateJetpackSiteOverRestOnly()
        assertTrue(jetpack1.isUsingWpComRestApi)

        val jetpack2 = SiteUtils.generateJetpackSiteOverXMLRPC()
        assertFalse(jetpack2.isUsingWpComRestApi)

        val pureSelfHosted1 = SiteUtils.generateSelfHostedNonJPSite()
        assertFalse(pureSelfHosted1.isUsingWpComRestApi)

        val pureSelfHosted2 = SiteUtils.generateSelfHostedSiteFutureJetpack()
        assertFalse(pureSelfHosted2.isUsingWpComRestApi)
    }

    @Test
    @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
    fun testRemoveWPComRestSitesAbsentFromList() {
        val allSites: MutableList<SiteModel> = ArrayList()
        val sitesToKeep: MutableList<SiteModel> = ArrayList()

        for (i in 0..14) {
            when (i % 3) {
                0 -> {
                    // add a .com site
                    val wpComSite = SiteUtils.generateWPComSite()
                    wpComSite.siteId = (i + 1).toLong()
                    wpComSite.url = "https://pony$i.com"
                    wpComSite.xmlRpcUrl = "https://pony$i.com/xmlrpc.php"
                    allSites.add(wpComSite)
                }

                1 -> {
                    // add a self-hosted Jetpack site
                    val jetpackSite = SiteUtils.generateJetpackSiteOverRestOnly()
                    jetpackSite.siteId = (i + 1).toLong()
                    jetpackSite.url = "https://pony$i.com"
                    jetpackSite.xmlRpcUrl = "https://pony$i.com/xmlrpc.php"
                    allSites.add(jetpackSite)
                }

                2 -> {
                    // add a self-hosted non-Jetpack site
                    val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()
                    selfHostedSite.siteId = (i + 1).toLong()
                    selfHostedSite.url = "https://pony$i.com"
                    selfHostedSite.xmlRpcUrl = "https://pony$i.com/xmlrpc.php"
                    allSites.add(selfHostedSite)
                }
            }
        }

        // add all sites to DB
        val createOrUpdateSites = SiteStore::class.java.getDeclaredMethod("createOrUpdateSites", SitesModel::class.java)
        createOrUpdateSites.isAccessible = true
        val res = createOrUpdateSites.invoke(mSiteStore, SitesModel(allSites)) as UpdateSitesResult?

        assertFalse(res!!.duplicateSiteFound)
        assertTrue(res.rowsAffected == 15)
        assertTrue(mSiteStore.sites.size == 15)

        // add 2 of each kind of site to keep
        sitesToKeep.addAll(allSites.subList(0, 6))

        // remove six sites (2/3 * (15 - 6))
        mSiteSqlUtils.removeWPComRestSitesAbsentFromList(sitesToKeep)

        assertTrue(mSiteStore.sites.size == 9)

        // make sure all sites in sitesToKeep are in the store
        for (site in sitesToKeep) {
            assertTrue(mSiteStore.getSiteBySiteId(site.siteId) != null)
        }
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertAndRetrieveForActiveModules() {
        val site = SiteUtils.generateWPComSite()
        val activeModules = (SiteModel.ACTIVE_MODULES_KEY_PUBLICIZE
            + ","
            + SiteModel.ACTIVE_MODULES_KEY_SHARING_BUTTONS)
        site.activeModules = activeModules

        mSiteStorePersistence.insertOrUpdateSite(site)

        val siteFromDb = mSiteSqlUtils.getSites()[0]
        assertTrue(siteFromDb.isActiveModuleEnabled(SiteModel.ACTIVE_MODULES_KEY_PUBLICIZE))
        assertTrue(siteFromDb.isActiveModuleEnabled(SiteModel.ACTIVE_MODULES_KEY_SHARING_BUTTONS))
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertAndRetrieveForPublicizePermanentlyDisabled() {
        val site = SiteUtils.generateWPComSite()
        site.setIsPublicizePermanentlyDisabled(true)

        mSiteStorePersistence.insertOrUpdateSite(site)

        val siteFromDb = mSiteSqlUtils.getSites()[0]
        assertTrue(siteFromDb.isPublicizePermanentlyDisabled)
    }

    @Test
    fun testZendeskPlanAndAddonsInsertionAndRetrieval() {
        val siteModel = SiteUtils.generateSiteWithZendeskMetaData()
        WellSql.insert(siteModel).execute()

        val siteFromDb = mSiteStore.sites[0]
        assertEquals(siteModel.zendeskPlan, siteFromDb.zendeskPlan)
        assertEquals(siteModel.zendeskAddOns, siteFromDb.zendeskAddOns)
    }
}
