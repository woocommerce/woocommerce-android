package org.wordpress.android.fluxc.store

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wellsql.generated.SiteModelTable
import com.yarolegovich.wellsql.WellSql
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.SitesModel
import org.wordpress.android.fluxc.network.rest.wpapi.site.SiteWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
import org.wordpress.android.fluxc.persistence.AccountMapper
import org.wordpress.android.fluxc.persistence.AccountStorePersistence
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.SiteStorePersistence
import org.wordpress.android.fluxc.persistence.SiteStorePersistence.DuplicateSiteException
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.persistence.domains.DomainDao
import org.wordpress.android.fluxc.site.SiteUtils
import org.wordpress.android.fluxc.store.SiteStore.UpdateSitesResult
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.lang.reflect.InvocationTargetException

@RunWith(RobolectricTestRunner::class)
class SiteStoreUnitTest {
    @Rule
    var wpDatabaseRule: WPDatabaseTestRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext<Context?>())

    private var mSiteSqlUtils: SiteSqlUtils? = null
    private var mSiteStorePersistence: SiteStorePersistence? = null
    private var mSiteStore: SiteStore? = null

    @Before
    fun setUp() {
        val config = WellSqlConfig(ApplicationProvider.getApplicationContext<Context?>())
        WellSql.init(config)
        config.reset()

        val accountStorePersistence = AccountStorePersistence(
            wpDatabaseRule.db,
            AccountMapper()
        )
        val account = AccountModel()
        account.setUserId(20151021)
        accountStorePersistence.insertOrUpdateDefaultAccount(account)

        mSiteSqlUtils = SiteSqlUtils()
        mSiteStorePersistence = SiteStorePersistence(accountStorePersistence)
        mSiteStore = SiteStore(
            Dispatcher(),
            Mockito.mock<SiteRestClient?>(SiteRestClient::class.java),
            Mockito.mock<SiteXMLRPCClient?>(SiteXMLRPCClient::class.java),
            Mockito.mock<SiteWPAPIRestClient?>(SiteWPAPIRestClient::class.java),
            mSiteSqlUtils!!,
            SiteStorePersistence(accountStorePersistence),
            Mockito.mock<DomainDao?>(DomainDao::class.java),
            initCoroutineEngine()
        )
    }

    @Test
    fun testSimpleInsertionAndRetrieval() {
        val siteModel = SiteModel()
        siteModel.setSiteId(42)
        WellSql.insert<SiteModel?>(siteModel).execute()

        Assert.assertEquals(1, mSiteStore!!.sites.size.toLong())

        Assert.assertEquals(42, mSiteStore!!.sites.get(0).getSiteId())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertOrUpdateSite() {
        val site = SiteUtils.generateWPComSite()
        mSiteStorePersistence!!.insertOrUpdateSite(site)

        Assert.assertTrue(mSiteStore!!.hasSiteWithLocalId(site.getId()))
        Assert.assertEquals(site.getSiteId(), mSiteStore!!.getSiteByLocalId(site.getId())!!.getSiteId())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testSelfHostedAndJetpackSites() {
        // Note: not using the helper methods to make sure of the SiteModel definition
        val ponySite = SiteModel()
        ponySite.setXmlRpcUrl("http://pony.com/xmlrpc.php")
        ponySite.setSiteId(1)
        ponySite.setIsWPCom(false)
        ponySite.setOrigin(SiteModel.ORIGIN_XMLRPC)
        mSiteStorePersistence!!.insertOrUpdateSite(ponySite)

        val jetpackOverXMLRPC = SiteModel()
        jetpackOverXMLRPC.setXmlRpcUrl("http://pony2.com/xmlrpc.php")
        jetpackOverXMLRPC.setSiteId(2)
        jetpackOverXMLRPC.setIsWPCom(false)
        jetpackOverXMLRPC.setIsJetpackInstalled(true)
        jetpackOverXMLRPC.setIsJetpackConnected(true)
        jetpackOverXMLRPC.setOrigin(SiteModel.ORIGIN_XMLRPC)
        mSiteStorePersistence!!.insertOrUpdateSite(jetpackOverXMLRPC)

        val jetpackOverRest = SiteModel()
        jetpackOverRest.setXmlRpcUrl("http://pony3.com/xmlrpc.php")
        jetpackOverRest.setSiteId(3)
        jetpackOverRest.setIsWPCom(false)
        jetpackOverRest.setIsJetpackInstalled(true)
        jetpackOverRest.setIsJetpackConnected(true)
        jetpackOverRest.setOrigin(SiteModel.ORIGIN_WPCOM_REST)
        mSiteStorePersistence!!.insertOrUpdateSite(jetpackOverRest)

        Assert.assertEquals(3, mSiteStore!!.sites.size.toLong())

        // User "install and connect" ponySite site to Jetpack via his connected .com account
        ponySite.setIsJetpackInstalled(true)
        ponySite.setIsJetpackConnected(true)
        ponySite.setOrigin(SiteModel.ORIGIN_WPCOM_REST)
        mSiteStorePersistence!!.insertOrUpdateSite(ponySite)

        Assert.assertEquals(3, mSiteStore!!.sites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testWPComSiteVisibility() {
        // Should not cause any errors
        mSiteStore!!.isWPComSiteVisibleByLocalId(45)
        mSiteSqlUtils!!.setSiteVisibility(null, true)

        val selfHostedNonJPSite = SiteUtils.generateSelfHostedNonJPSite()
        mSiteStorePersistence!!.insertOrUpdateSite(selfHostedNonJPSite)

        // Attempt to use with id of self-hosted site
        mSiteSqlUtils!!.setSiteVisibility(selfHostedNonJPSite, false)
        // The self-hosted site should not be affected
        Assert.assertTrue(mSiteStore!!.getSiteByLocalId(selfHostedNonJPSite.getId())!!.isVisible())


        val wpComSite = SiteUtils.generateWPComSite()
        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite)

        // Attempt to use with legitimate .com site
        mSiteSqlUtils!!.setSiteVisibility(selfHostedNonJPSite, false)
        Assert.assertFalse(mSiteStore!!.getSiteByLocalId(wpComSite.getId())!!.isVisible())
        Assert.assertFalse(mSiteStore!!.isWPComSiteVisibleByLocalId(wpComSite.getId()))
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testSetAllWPComSitesVisibility() {
        val selfHostedNonJPSite = SiteUtils.generateSelfHostedNonJPSite()
        mSiteStorePersistence!!.insertOrUpdateSite(selfHostedNonJPSite)

        // Attempt to use with id of self-hosted site
        for (site in mSiteSqlUtils!!.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel()) {
            mSiteSqlUtils!!.setSiteVisibility(site, false)
        }
        // The self-hosted site should not be affected
        Assert.assertTrue(mSiteStore!!.getSiteByLocalId(selfHostedNonJPSite.getId())!!.isVisible())

        val wpComSite1 = SiteUtils.generateWPComSite()
        val wpComSite2 = SiteUtils.generateWPComSite()
        wpComSite2.setId(44)
        wpComSite2.setSiteId(284)

        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite1)
        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite2)

        // Attempt to use with legitimate .com site
        for (site in mSiteSqlUtils!!.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel()) {
            mSiteSqlUtils!!.setSiteVisibility(site, false)
        }
        Assert.assertTrue(mSiteStore!!.getSiteByLocalId(selfHostedNonJPSite.getId())!!.isVisible())
        Assert.assertFalse(mSiteStore!!.getSiteByLocalId(wpComSite1.getId())!!.isVisible())
        Assert.assertFalse(mSiteStore!!.getSiteByLocalId(wpComSite2.getId())!!.isVisible())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testGetIdForIdMethods() {
        Assert.assertEquals(0, mSiteStore!!.getLocalIdForRemoteSiteId(555).toLong())
        Assert.assertEquals(0, mSiteStore!!.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(2626, "").toLong())
        Assert.assertEquals(0, mSiteStore!!.getSiteIdForLocalId(5577))

        val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()
        val wpComSite = SiteUtils.generateWPComSite()
        val jetpackSite = SiteUtils.generateJetpackSiteOverXMLRPC()
        mSiteStorePersistence!!.insertOrUpdateSite(selfHostedSite)
        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite)
        mSiteStorePersistence!!.insertOrUpdateSite(jetpackSite)

        Assert.assertEquals(
            selfHostedSite.getId().toLong(),
            mSiteStore!!.getLocalIdForRemoteSiteId(selfHostedSite.getSelfHostedSiteId()).toLong()
        )
        Assert.assertEquals(
            wpComSite.getId().toLong(),
            mSiteStore!!.getLocalIdForRemoteSiteId(wpComSite.getSiteId()).toLong()
        )

        // Should be able to look up a Jetpack site by .com and by .org id (assuming it's been set)
        Assert.assertEquals(
            jetpackSite.getId().toLong(),
            mSiteStore!!.getLocalIdForRemoteSiteId(jetpackSite.getSiteId()).toLong()
        )
        Assert.assertEquals(
            jetpackSite.getId().toLong(),
            mSiteStore!!.getLocalIdForRemoteSiteId(jetpackSite.getSelfHostedSiteId()).toLong()
        )

        Assert.assertEquals(
            selfHostedSite.getId().toLong(), mSiteStore!!.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(
                selfHostedSite.getSelfHostedSiteId(), selfHostedSite.getXmlRpcUrl()
            ).toLong()
        )
        Assert.assertEquals(
            jetpackSite.getId().toLong(), mSiteStore!!.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(
                jetpackSite.getSelfHostedSiteId(), jetpackSite.getXmlRpcUrl()
            ).toLong()
        )

        Assert.assertEquals(
            selfHostedSite.getSelfHostedSiteId(),
            mSiteStore!!.getSiteIdForLocalId(selfHostedSite.getId())
        )
        Assert.assertEquals(wpComSite.getSiteId(), mSiteStore!!.getSiteIdForLocalId(wpComSite.getId()))
        Assert.assertEquals(jetpackSite.getSiteId(), mSiteStore!!.getSiteIdForLocalId(jetpackSite.getId()))
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testGetSiteBySiteId() {
        Assert.assertNull(mSiteStore!!.getSiteBySiteId(555))

        val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()
        val wpComSite = SiteUtils.generateWPComSite()
        val jetpackSiteOverXMLRPC = SiteUtils.generateJetpackSiteOverXMLRPC()
        mSiteStorePersistence!!.insertOrUpdateSite(selfHostedSite)
        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite)
        mSiteStorePersistence!!.insertOrUpdateSite(jetpackSiteOverXMLRPC)

        Assert.assertEquals(1, mSiteSqlUtils!!.sitesAccessedViaWPComRest.getAsCursor().getCount().toLong())
        Assert.assertNotNull(mSiteStore!!.getSiteBySiteId(wpComSite.getSiteId()))
        Assert.assertNotNull(mSiteStore!!.getSiteBySiteId(jetpackSiteOverXMLRPC.getSiteId()))
        Assert.assertNull(mSiteStore!!.getSiteBySiteId(selfHostedSite.getSiteId()))
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testDeleteSite() {
        val wpComSite = SiteUtils.generateWPComSite()

        // Should not cause any errors
        mSiteSqlUtils!!.deleteSite(wpComSite)

        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite)
        val affectedRows = mSiteSqlUtils!!.deleteSite(wpComSite)

        Assert.assertEquals(1, affectedRows.toLong())
        Assert.assertEquals(0, mSiteStore!!.sites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testGetWPComSites() {
        val wpComSite = SiteUtils.generateWPComSite()
        val jetpackSiteOverXMLRPC = SiteUtils.generateJetpackSiteOverXMLRPC()
        val jetpackSiteOverRestOnly = SiteUtils.generateJetpackSiteOverRestOnly()

        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite)
        mSiteStorePersistence!!.insertOrUpdateSite(jetpackSiteOverXMLRPC)
        mSiteStorePersistence!!.insertOrUpdateSite(jetpackSiteOverRestOnly)

        Assert.assertEquals(2, mSiteSqlUtils!!.sitesAccessedViaWPComRest.getAsCursor().getCount().toLong())

        val wpComSites = mSiteSqlUtils!!.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel()
        Assert.assertEquals(1, wpComSites.size.toLong())
        for (site in wpComSites) {
            Assert.assertNotEquals(jetpackSiteOverXMLRPC.getId().toLong(), site.getId().toLong())
        }
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertDuplicateSites() {
        val futureJetpack = SiteUtils.generateSelfHostedSiteFutureJetpack()
        val jetpack = SiteUtils.generateJetpackSiteOverRestOnly()

        // Insert a self hosted site that will later be converted to Jetpack
        mSiteStorePersistence!!.insertOrUpdateSite(futureJetpack)

        // Insert the same site but Jetpack powered this time
        mSiteStorePersistence!!.insertOrUpdateSite(jetpack)

        // Previous site should be converted to a Jetpack site and we should see only one site
        val sitesCount = WellSql.select<SiteModel?>(SiteModel::class.java).getAsCursor().getCount()
        Assert.assertEquals(1, sitesCount.toLong())

        val wpComSites = mSiteSqlUtils!!.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel()
        Assert.assertEquals(0, wpComSites.size.toLong())
        Assert.assertEquals(1, mSiteSqlUtils!!.sitesAccessedViaWPComRest.getAsCursor().getCount().toLong())
        val jetpackSites =
            mSiteSqlUtils!!.getSitesWith(SiteModelTable.IS_JETPACK_CONNECTED, true).getAsModel()
        Assert.assertEquals(jetpack.getSiteId(), jetpackSites.get(0)!!.getSiteId())
        Assert.assertTrue(jetpackSites.get(0)!!.isJetpackConnected())
        Assert.assertFalse(jetpackSites.get(0)!!.isWPCom())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertDuplicateSitesError() {
        val futureJetpack = SiteUtils.generateSelfHostedSiteFutureJetpack()
        val jetpack = SiteUtils.generateJetpackSiteOverRestOnly()

        // Insert a Jetpack powered site
        mSiteStorePersistence!!.insertOrUpdateSite(jetpack)
        var duplicate = false
        try {
            // Insert the same site but via self hosted this time (this should fail)
            mSiteStorePersistence!!.insertOrUpdateSite(futureJetpack)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        Assert.assertTrue(duplicate)
        val sitesCount = WellSql.select<SiteModel?>(SiteModel::class.java).getAsCursor().getCount()
        Assert.assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertDuplicateSitesDifferentSchemesError1() {
        val futureJetpack = SiteUtils.generateSelfHostedSiteFutureJetpack()
        val jetpack = SiteUtils.generateJetpackSiteOverRestOnly()

        futureJetpack.setXmlRpcUrl("https://pony.com/xmlrpc.php")
        jetpack.setXmlRpcUrl("http://pony.com/xmlrpc.php")

        // Insert a Jetpack powered site
        mSiteStorePersistence!!.insertOrUpdateSite(jetpack)
        var duplicate = false
        try {
            // Insert the same site but via self hosted this time (this should fail)
            mSiteStorePersistence!!.insertOrUpdateSite(futureJetpack)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        Assert.assertTrue(duplicate)
        val sitesCount = WellSql.select<SiteModel?>(SiteModel::class.java).getAsCursor().getCount()
        Assert.assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertDuplicateSitesDifferentSchemesError2() {
        val futureJetpack = SiteUtils.generateSelfHostedSiteFutureJetpack()
        val jetpack = SiteUtils.generateJetpackSiteOverRestOnly()

        futureJetpack.setXmlRpcUrl("http://pony.com/xmlrpc.php")
        jetpack.setXmlRpcUrl("https://pony.com/xmlrpc.php")

        // Insert a Jetpack powered site
        mSiteStorePersistence!!.insertOrUpdateSite(jetpack)
        var duplicate = false
        try {
            // Insert the same site but via self hosted this time (this should fail)
            mSiteStorePersistence!!.insertOrUpdateSite(futureJetpack)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        Assert.assertTrue(duplicate)
        val sitesCount = WellSql.select<SiteModel?>(SiteModel::class.java).getAsCursor().getCount()
        Assert.assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertDuplicateXmlRpcJetpackSite() {
        val jetpackXmlRpcSite = SiteUtils.generateJetpackSiteOverXMLRPC()

        jetpackXmlRpcSite.setUrl("http://some.url")

        // Insert a Jetpack powered site over XML-RPC
        mSiteStorePersistence!!.insertOrUpdateSite(jetpackXmlRpcSite)

        // Set up the same site (by URL/XML-RPC URL), but don't identify it as a Jetpack site
        // This simulates sites resulting from wp.getUsersBlogs, which don't have the site ID and can't be identified
        // as Jetpack or not (wp.getOptions is the call that returns that information)
        val jetpackXmlRpcSite2 = SiteUtils.generateSelfHostedNonJPSite()
        jetpackXmlRpcSite2.setXmlRpcUrl(jetpackXmlRpcSite.getXmlRpcUrl())
        jetpackXmlRpcSite2.setUrl(jetpackXmlRpcSite.getUrl())
        jetpackXmlRpcSite2.setSelfHostedSiteId(jetpackXmlRpcSite.getSelfHostedSiteId())
        jetpackXmlRpcSite2.setUsername(jetpackXmlRpcSite.getUsername())
        jetpackXmlRpcSite2.setPassword(jetpackXmlRpcSite.getPassword())

        var duplicate = false
        try {
            // Insert the same site but not identified as a Jetpack site
            // (this should succeed, replacing the existing site, because the site replaced is not using the REST API)
            mSiteStorePersistence!!.insertOrUpdateSite(jetpackXmlRpcSite2)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        Assert.assertFalse(duplicate)
        val sitesCount = WellSql.select<SiteModel?>(SiteModel::class.java).getAsCursor().getCount()
        Assert.assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testSearchSitesByNameMatching() {
        val wpComSite1 = SiteUtils.generateWPComSite()
        wpComSite1.setName("Doctor Emmet Brown Homepage")
        val wpComSite2 = SiteUtils.generateWPComSite()
        wpComSite2.setName("Shield Eyes from light")
        wpComSite2.setSiteId(557)
        val wpComSite3 = SiteUtils.generateWPComSite()
        wpComSite3.setName("I remember when this was all farmland as far as the eye could see")
        wpComSite2.setSiteId(558)

        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite1)
        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite2)
        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite3)

        var matchingSites: MutableList<SiteModel?> = mSiteSqlUtils!!.getSitesByNameOrUrlMatching("eye")
        Assert.assertEquals(2, matchingSites.size.toLong())

        matchingSites = mSiteSqlUtils!!.getSitesByNameOrUrlMatching("EYE")
        Assert.assertEquals(2, matchingSites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testSearchSitesByNameOrUrlMatching() {
        val wpComSite1 = SiteUtils.generateWPComSite()
        wpComSite1.setName("Doctor Emmet Brown Homepage")
        val wpComSite2 = SiteUtils.generateWPComSite()
        wpComSite2.setUrl("shieldeyesfromlight.wordpress.com")
        val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()
        selfHostedSite.setName("I remember when this was all farmland as far as the eye could see.")

        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite1)
        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite2)
        mSiteStorePersistence!!.insertOrUpdateSite(selfHostedSite)

        var matchingSites: MutableList<SiteModel?> = mSiteSqlUtils!!.getSitesByNameOrUrlMatching("eye")
        Assert.assertEquals(2, matchingSites.size.toLong())

        matchingSites = mSiteSqlUtils!!.getSitesByNameOrUrlMatching("EYE")
        Assert.assertEquals(2, matchingSites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testSearchWPComSitesByNameOrUrlMatching() {
        val wpComSite1 = SiteUtils.generateWPComSite()
        wpComSite1.setName("Doctor Emmet Brown Homepage")
        val wpComSite2 = SiteUtils.generateWPComSite()
        wpComSite2.setUrl("shieldeyesfromlight.wordpress.com")
        val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()
        selfHostedSite.setName("I remember when this was all farmland as far as the eye could see.")

        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite1)
        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite2)
        mSiteStorePersistence!!.insertOrUpdateSite(selfHostedSite)

        var matchingSites: MutableList<SiteModel?> =
            mSiteSqlUtils!!.getSitesAccessedViaWPComRestByNameOrUrlMatching("eye")
        Assert.assertEquals(1, matchingSites.size.toLong())

        matchingSites = mSiteSqlUtils!!.getSitesAccessedViaWPComRestByNameOrUrlMatching("EYE")
        Assert.assertEquals(1, matchingSites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testRemoveAllSites() {
        val wpComSite = SiteUtils.generateWPComSite()
        val jetpackXMLRPCSite = SiteUtils.generateJetpackSiteOverXMLRPC()
        val jetpackRestSite = SiteUtils.generateJetpackSiteOverRestOnly()
        val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()

        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite)
        mSiteStorePersistence!!.insertOrUpdateSite(jetpackXMLRPCSite)
        mSiteStorePersistence!!.insertOrUpdateSite(jetpackRestSite)
        mSiteStorePersistence!!.insertOrUpdateSite(selfHostedSite)

        // first make sure sites are inserted successfully
        Assert.assertEquals(4, mSiteStore!!.sites.size.toLong())

        mSiteSqlUtils!!.deleteAllSites()

        Assert.assertEquals(0, mSiteStore!!.sites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testWPComAutomatedTransfer() {
        val wpComSite = SiteUtils.generateWPComSite()
        mSiteStorePersistence!!.insertOrUpdateSite(wpComSite)

        // Turn WP.com site into an Automated Transfer (Jetpack) site
        val automatedTransferSite = SiteUtils.generateWPComSite()
        automatedTransferSite.setIsJetpackInstalled(true)
        automatedTransferSite.setIsJetpackConnected(true)
        automatedTransferSite.setIsWPCom(false)
        automatedTransferSite.setIsAutomatedTransfer(true)

        mSiteStorePersistence!!.insertOrUpdateSite(automatedTransferSite)

        Assert.assertEquals(1, mSiteStore!!.sites.size.toLong())
    }

    @Test
    @Throws(NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class)
    fun testBatchInsertSiteNoDuplicateWPCom() {
        val siteList: MutableList<SiteModel?> = ArrayList<SiteModel?>()
        siteList.add(SiteUtils.generateTestSite(1, "https://pony1.com", "https://pony1.com/xmlrpc.php", true, true))
        siteList.add(SiteUtils.generateTestSite(2, "https://pony2.com", "https://pony2.com/xmlrpc.php", true, true))
        siteList.add(SiteUtils.generateTestSite(3, "https://pony3.com", "https://pony3.com/xmlrpc.php", true, true))
        siteList.add(SiteUtils.generateTestSite(4, "https://pony4.com", "https://pony4.com/xmlrpc.php", true, true))
        siteList.add(SiteUtils.generateTestSite(5, "https://pony5.com", "https://pony5.com/xmlrpc.php", true, true))

        val sites = SitesModel(siteList)

        // Use reflection to call a private Store method: equivalent to mSiteStore.updateSites(sites)
        val createOrUpdateSites = SiteStore::class.java.getDeclaredMethod("createOrUpdateSites", SitesModel::class.java)
        createOrUpdateSites.setAccessible(true)
        val res = createOrUpdateSites.invoke(mSiteStore, sites) as UpdateSitesResult?

        Assert.assertFalse(res!!.duplicateSiteFound)
        Assert.assertEquals(5, res.rowsAffected.toLong())
        Assert.assertEquals(5, mSiteStore!!.sites.size.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertSiteDuplicateXmlRpcTrailingSlash() {
        // It's possible for the URL in `wp.getOptions` to be different from the URL in `wp.getUsersBlogs`,
        // sometimes just by a trailing slash
        // This test checks that we can still identify two sites as being identical in this case, and that we quietly
        // update the existing site rather than throw a duplicate site exception
        val selfhostedSite = SiteUtils.generateSelfHostedNonJPSite()
        selfhostedSite.setUrl("http://some.url")

        mSiteStorePersistence!!.insertOrUpdateSite(selfhostedSite)

        val selfhostedSite2 = SiteUtils.generateSelfHostedNonJPSite()
        selfhostedSite2.setUrl("http://some.url/")

        var duplicate = false
        try {
            // Insert the same site with a trailing slash (this should succeed, replacing the existing site)
            mSiteStorePersistence!!.insertOrUpdateSite(selfhostedSite2)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        Assert.assertFalse(duplicate)
        val sitesCount = WellSql.select<SiteModel?>(SiteModel::class.java).getAsCursor().getCount()
        Assert.assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertSiteDuplicateXmlRpcDifferentUrl() {
        // It's possible for the URL in `wp.getOptions` to be different from the URL in `wp.getUsersBlogs`
        // This test checks that we can still identify two sites as being identical in this case, and that we quietly
        // update the existing site rather than throw a duplicate site exception
        val selfhostedSite = SiteUtils.generateSelfHostedNonJPSite()
        selfhostedSite.setUrl("http://some.url")
        selfhostedSite.setXmlRpcUrl("http://some.url/xmlrpc.php")

        mSiteStorePersistence!!.insertOrUpdateSite(selfhostedSite)

        val selfhostedSite2 = SiteUtils.generateSelfHostedNonJPSite()
        selfhostedSite2.setUrl("http://user5242.stagingsite.url")
        selfhostedSite2.setXmlRpcUrl("http://some.url/xmlrpc.php")

        var duplicate = false
        try {
            // Insert the same site with a different URL, but the same XML-RPC URL
            // (this should succeed, replacing the existing site)
            mSiteStorePersistence!!.insertOrUpdateSite(selfhostedSite2)
        } catch (e: DuplicateSiteException) {
            // Caught !
            duplicate = true
        }
        Assert.assertFalse(duplicate)
        val sitesCount = WellSql.select<SiteModel?>(SiteModel::class.java).getAsCursor().getCount()
        Assert.assertEquals(1, sitesCount.toLong())
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testUpdateSiteUniqueConstraintFail() {
        // Create 2 test sites
        val site1 = SiteUtils.generateTestSite(0, "https://pony1.com", "https://pony1.com/xmlrpc.php", false, true)
        mSiteStorePersistence!!.insertOrUpdateSite(site1)
        val site2 = SiteUtils.generateTestSite(0, "https://pony2.com", "https://pony2.com/xmlrpc.php", false, true)
        mSiteStorePersistence!!.insertOrUpdateSite(site2)

        // Update the second site and reuse the site url and id from the first
        site2.setUrl("https://pony1.com")
        var duplicate = false
        try {
            mSiteStorePersistence!!.insertOrUpdateSite(site2)
        } catch (e: DuplicateSiteException) {
            duplicate = true
        }
        Assert.assertTrue(duplicate)
    }

    @Test
    fun testJetpackSelfHostedAndForceXMLRPC() {
        val jetpackSite = SiteUtils.generateJetpackSiteOverXMLRPC()
        jetpackSite.setOrigin(SiteModel.ORIGIN_WPCOM_REST)
        Assert.assertTrue(jetpackSite.isUsingWpComRestApi())

        // Force the origin, it should now use XMLRPC instead of REST.
        jetpackSite.setOrigin(SiteModel.ORIGIN_XMLRPC)
        Assert.assertFalse(jetpackSite.isUsingWpComRestApi())
    }

    @Test
    fun testDefaultUsageWpComRestApi() {
        val wpComSite = SiteUtils.generateWPComSite()
        Assert.assertTrue(wpComSite.isUsingWpComRestApi())

        val jetpack1 = SiteUtils.generateJetpackSiteOverRestOnly()
        Assert.assertTrue(jetpack1.isUsingWpComRestApi())

        val jetpack2 = SiteUtils.generateJetpackSiteOverXMLRPC()
        Assert.assertFalse(jetpack2.isUsingWpComRestApi())

        val pureSelfHosted1 = SiteUtils.generateSelfHostedNonJPSite()
        Assert.assertFalse(pureSelfHosted1.isUsingWpComRestApi())

        val pureSelfHosted2 = SiteUtils.generateSelfHostedSiteFutureJetpack()
        Assert.assertFalse(pureSelfHosted2.isUsingWpComRestApi())
    }

    @Test
    @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
    fun testRemoveWPComRestSitesAbsentFromList() {
        val allSites: MutableList<SiteModel?> = ArrayList<SiteModel?>()
        val sitesToKeep: MutableList<SiteModel> = ArrayList<SiteModel>()

        for (i in 0..14) {
            when (i % 3) {
                0 -> {
                    // add a .com site
                    val wpComSite = SiteUtils.generateWPComSite()
                    wpComSite.setSiteId((i + 1).toLong())
                    wpComSite.setUrl("https://pony" + i + ".com")
                    wpComSite.setXmlRpcUrl("https://pony" + i + ".com/xmlrpc.php")
                    allSites.add(wpComSite)
                }

                1 -> {
                    // add a self-hosted Jetpack site
                    val jetpackSite = SiteUtils.generateJetpackSiteOverRestOnly()
                    jetpackSite.setSiteId((i + 1).toLong())
                    jetpackSite.setUrl("https://pony" + i + ".com")
                    jetpackSite.setXmlRpcUrl("https://pony" + i + ".com/xmlrpc.php")
                    allSites.add(jetpackSite)
                }

                2 -> {
                    // add a self-hosted non-Jetpack site
                    val selfHostedSite = SiteUtils.generateSelfHostedNonJPSite()
                    selfHostedSite.setSiteId((i + 1).toLong())
                    selfHostedSite.setUrl("https://pony" + i + ".com")
                    selfHostedSite.setXmlRpcUrl("https://pony" + i + ".com/xmlrpc.php")
                    allSites.add(selfHostedSite)
                }
            }
        }

        // add all sites to DB
        val createOrUpdateSites = SiteStore::class.java.getDeclaredMethod("createOrUpdateSites", SitesModel::class.java)
        createOrUpdateSites.setAccessible(true)
        val res = createOrUpdateSites.invoke(mSiteStore, SitesModel(allSites)) as UpdateSitesResult?

        Assert.assertFalse(res!!.duplicateSiteFound)
        Assert.assertTrue(res.rowsAffected == 15)
        Assert.assertTrue(mSiteStore!!.sites.size == 15)

        // add 2 of each kind of site to keep
        sitesToKeep.addAll(allSites.subList(0, 6))

        // remove six sites (2/3 * (15 - 6))
        mSiteSqlUtils!!.removeWPComRestSitesAbsentFromList(sitesToKeep)

        Assert.assertTrue(mSiteStore!!.sites.size == 9)

        // make sure all sites in sitesToKeep are in the store
        for (site in sitesToKeep) {
            Assert.assertTrue(mSiteStore!!.getSiteBySiteId(site.getSiteId()) != null)
        }
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertAndRetrieveForActiveModules() {
        val site = SiteUtils.generateWPComSite()
        val activeModules = (SiteModel.ACTIVE_MODULES_KEY_PUBLICIZE
                + ","
                + SiteModel.ACTIVE_MODULES_KEY_SHARING_BUTTONS)
        site.setActiveModules(activeModules)

        mSiteStorePersistence!!.insertOrUpdateSite(site)

        val siteFromDb = mSiteSqlUtils!!.getSites().get(0)
        Assert.assertTrue(siteFromDb.isActiveModuleEnabled(SiteModel.ACTIVE_MODULES_KEY_PUBLICIZE))
        Assert.assertTrue(siteFromDb.isActiveModuleEnabled(SiteModel.ACTIVE_MODULES_KEY_SHARING_BUTTONS))
    }

    @Test
    @Throws(DuplicateSiteException::class)
    fun testInsertAndRetrieveForPublicizePermanentlyDisabled() {
        val site = SiteUtils.generateWPComSite()
        site.setIsPublicizePermanentlyDisabled(true)

        mSiteStorePersistence!!.insertOrUpdateSite(site)

        val siteFromDb = mSiteSqlUtils!!.getSites().get(0)
        Assert.assertTrue(siteFromDb.isPublicizePermanentlyDisabled())
    }

    @Test
    fun testZendeskPlanAndAddonsInsertionAndRetrieval() {
        val siteModel = SiteUtils.generateSiteWithZendeskMetaData()
        WellSql.insert<SiteModel?>(siteModel).execute()

        val siteFromDb = mSiteStore!!.sites.get(0)
        Assert.assertEquals(siteModel.getZendeskPlan(), siteFromDb.getZendeskPlan())
        Assert.assertEquals(siteModel.getZendeskAddOns(), siteFromDb.getZendeskAddOns())
    }
}
