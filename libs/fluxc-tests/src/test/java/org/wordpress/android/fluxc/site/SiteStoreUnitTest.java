package org.wordpress.android.fluxc.site;

import android.content.Context;

import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.rest.wpapi.site.SiteWPAPIRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.persistence.AccountMapper;
import org.wordpress.android.fluxc.persistence.AccountStorePersistence;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils.DuplicateSiteException;
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.persistence.domains.DomainDao;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.UpdateSitesResult;
import org.wordpress.android.fluxc.tools.CoroutineEngineUtilsKt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.wordpress.android.fluxc.site.SiteUtils.generateJetpackSiteOverRestOnly;
import static org.wordpress.android.fluxc.site.SiteUtils.generateJetpackSiteOverXMLRPC;
import static org.wordpress.android.fluxc.site.SiteUtils.generateSelfHostedNonJPSite;
import static org.wordpress.android.fluxc.site.SiteUtils.generateSelfHostedSiteFutureJetpack;
import static org.wordpress.android.fluxc.site.SiteUtils.generateSiteWithZendeskMetaData;
import static org.wordpress.android.fluxc.site.SiteUtils.generateTestSite;
import static org.wordpress.android.fluxc.site.SiteUtils.generateWPComSite;

@RunWith(RobolectricTestRunner.class)
public class SiteStoreUnitTest {
    @Rule
    public WPDatabaseTestRule wpDatabaseRule = new WPDatabaseTestRule(ApplicationProvider.getApplicationContext());

    private SiteSqlUtils mSiteSqlUtils;
    private SiteStore mSiteStore;

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();

        AccountStorePersistence accountStorePersistence = new AccountStorePersistence(
                wpDatabaseRule.getDb(),
                new AccountMapper()
        );
        AccountModel account = new AccountModel();
        account.setUserId(20151021);
        accountStorePersistence.insertOrUpdateDefaultAccount(account);

        mSiteSqlUtils = new SiteSqlUtils(accountStorePersistence);
        mSiteStore = new SiteStore(
                new Dispatcher(),
                Mockito.mock(SiteRestClient.class),
                Mockito.mock(SiteXMLRPCClient.class),
                Mockito.mock(SiteWPAPIRestClient.class),
                mSiteSqlUtils,
                Mockito.mock(DomainDao.class),
                CoroutineEngineUtilsKt.initCoroutineEngine()
        );
    }

    @Test
    public void testSimpleInsertionAndRetrieval() {
        SiteModel siteModel = new SiteModel();
        siteModel.setSiteId(42);
        WellSql.insert(siteModel).execute();

        assertEquals(1, mSiteStore.getSites().size());

        assertEquals(42, mSiteStore.getSites().get(0).getSiteId());
    }

    @Test
    public void testInsertOrUpdateSite() throws DuplicateSiteException {
        SiteModel site = generateWPComSite();
        mSiteSqlUtils.insertOrUpdateSite(site);

        assertTrue(mSiteStore.hasSiteWithLocalId(site.getId()));
        assertEquals(site.getSiteId(), mSiteStore.getSiteByLocalId(site.getId()).getSiteId());
    }

    @Test
    public void testSelfHostedAndJetpackSites() throws DuplicateSiteException {
        // Note: not using the helper methods to make sure of the SiteModel definition
        SiteModel ponySite = new SiteModel();
        ponySite.setXmlRpcUrl("http://pony.com/xmlrpc.php");
        ponySite.setSiteId(1);
        ponySite.setIsWPCom(false);
        ponySite.setOrigin(SiteModel.ORIGIN_XMLRPC);
        mSiteSqlUtils.insertOrUpdateSite(ponySite);

        SiteModel jetpackOverXMLRPC = new SiteModel();
        jetpackOverXMLRPC.setXmlRpcUrl("http://pony2.com/xmlrpc.php");
        jetpackOverXMLRPC.setSiteId(2);
        jetpackOverXMLRPC.setIsWPCom(false);
        jetpackOverXMLRPC.setIsJetpackInstalled(true);
        jetpackOverXMLRPC.setIsJetpackConnected(true);
        jetpackOverXMLRPC.setOrigin(SiteModel.ORIGIN_XMLRPC);
        mSiteSqlUtils.insertOrUpdateSite(jetpackOverXMLRPC);

        SiteModel jetpackOverRest = new SiteModel();
        jetpackOverRest.setXmlRpcUrl("http://pony3.com/xmlrpc.php");
        jetpackOverRest.setSiteId(3);
        jetpackOverRest.setIsWPCom(false);
        jetpackOverRest.setIsJetpackInstalled(true);
        jetpackOverRest.setIsJetpackConnected(true);
        jetpackOverRest.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        mSiteSqlUtils.insertOrUpdateSite(jetpackOverRest);

        assertEquals(3, mSiteStore.getSites().size());

        // User "install and connect" ponySite site to Jetpack via his connected .com account

        ponySite.setIsJetpackInstalled(true);
        ponySite.setIsJetpackConnected(true);
        ponySite.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        mSiteSqlUtils.insertOrUpdateSite(ponySite);

        assertEquals(3, mSiteStore.getSites().size());
    }

    @Test
    public void testWPComSiteVisibility() throws DuplicateSiteException {
        // Should not cause any errors
        mSiteStore.isWPComSiteVisibleByLocalId(45);
        mSiteSqlUtils.setSiteVisibility(null, true);

        SiteModel selfHostedNonJPSite = generateSelfHostedNonJPSite();
        mSiteSqlUtils.insertOrUpdateSite(selfHostedNonJPSite);

        // Attempt to use with id of self-hosted site
        mSiteSqlUtils.setSiteVisibility(selfHostedNonJPSite, false);
        // The self-hosted site should not be affected
        assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.getId()).isVisible());


        SiteModel wpComSite = generateWPComSite();
        mSiteSqlUtils.insertOrUpdateSite(wpComSite);

        // Attempt to use with legitimate .com site
        mSiteSqlUtils.setSiteVisibility(selfHostedNonJPSite, false);
        assertFalse(mSiteStore.getSiteByLocalId(wpComSite.getId()).isVisible());
        assertFalse(mSiteStore.isWPComSiteVisibleByLocalId(wpComSite.getId()));
    }

    @Test
    public void testSetAllWPComSitesVisibility() throws DuplicateSiteException {
        SiteModel selfHostedNonJPSite = generateSelfHostedNonJPSite();
        mSiteSqlUtils.insertOrUpdateSite(selfHostedNonJPSite);

        // Attempt to use with id of self-hosted site
        for (SiteModel site : mSiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel()) {
            mSiteSqlUtils.setSiteVisibility(site, false);
        }
        // The self-hosted site should not be affected
        assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.getId()).isVisible());

        SiteModel wpComSite1 = generateWPComSite();
        SiteModel wpComSite2 = generateWPComSite();
        wpComSite2.setId(44);
        wpComSite2.setSiteId(284);

        mSiteSqlUtils.insertOrUpdateSite(wpComSite1);
        mSiteSqlUtils.insertOrUpdateSite(wpComSite2);

        // Attempt to use with legitimate .com site
        for (SiteModel site : mSiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel()) {
            mSiteSqlUtils.setSiteVisibility(site, false);
        }
        assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.getId()).isVisible());
        assertFalse(mSiteStore.getSiteByLocalId(wpComSite1.getId()).isVisible());
        assertFalse(mSiteStore.getSiteByLocalId(wpComSite2.getId()).isVisible());
    }

    @Test
    public void testGetIdForIdMethods() throws DuplicateSiteException {
        assertEquals(0, mSiteStore.getLocalIdForRemoteSiteId(555));
        assertEquals(0, mSiteStore.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(2626, ""));
        assertEquals(0, mSiteStore.getSiteIdForLocalId(5577));

        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        SiteModel wpComSite = generateWPComSite();
        SiteModel jetpackSite = generateJetpackSiteOverXMLRPC();
        mSiteSqlUtils.insertOrUpdateSite(selfHostedSite);
        mSiteSqlUtils.insertOrUpdateSite(wpComSite);
        mSiteSqlUtils.insertOrUpdateSite(jetpackSite);

        assertEquals(selfHostedSite.getId(),
                mSiteStore.getLocalIdForRemoteSiteId(selfHostedSite.getSelfHostedSiteId()));
        assertEquals(wpComSite.getId(), mSiteStore.getLocalIdForRemoteSiteId(wpComSite.getSiteId()));

        // Should be able to look up a Jetpack site by .com and by .org id (assuming it's been set)
        assertEquals(jetpackSite.getId(), mSiteStore.getLocalIdForRemoteSiteId(jetpackSite.getSiteId()));
        assertEquals(jetpackSite.getId(), mSiteStore.getLocalIdForRemoteSiteId(jetpackSite.getSelfHostedSiteId()));

        assertEquals(selfHostedSite.getId(), mSiteStore.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(
                selfHostedSite.getSelfHostedSiteId(), selfHostedSite.getXmlRpcUrl()));
        assertEquals(jetpackSite.getId(), mSiteStore.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(
                jetpackSite.getSelfHostedSiteId(), jetpackSite.getXmlRpcUrl()));

        assertEquals(selfHostedSite.getSelfHostedSiteId(), mSiteStore.getSiteIdForLocalId(selfHostedSite.getId()));
        assertEquals(wpComSite.getSiteId(), mSiteStore.getSiteIdForLocalId(wpComSite.getId()));
        assertEquals(jetpackSite.getSiteId(), mSiteStore.getSiteIdForLocalId(jetpackSite.getId()));
    }

    @Test
    public void testGetSiteBySiteId() throws DuplicateSiteException {
        assertNull(mSiteStore.getSiteBySiteId(555));

        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        SiteModel wpComSite = generateWPComSite();
        SiteModel jetpackSiteOverXMLRPC = generateJetpackSiteOverXMLRPC();
        mSiteSqlUtils.insertOrUpdateSite(selfHostedSite);
        mSiteSqlUtils.insertOrUpdateSite(wpComSite);
        mSiteSqlUtils.insertOrUpdateSite(jetpackSiteOverXMLRPC);

        assertEquals(1, mSiteSqlUtils.getSitesAccessedViaWPComRest().getAsCursor().getCount());
        assertNotNull(mSiteStore.getSiteBySiteId(wpComSite.getSiteId()));
        assertNotNull(mSiteStore.getSiteBySiteId(jetpackSiteOverXMLRPC.getSiteId()));
        assertNull(mSiteStore.getSiteBySiteId(selfHostedSite.getSiteId()));
    }

    @Test
    public void testDeleteSite() throws DuplicateSiteException {
        SiteModel wpComSite = generateWPComSite();

        // Should not cause any errors
        mSiteSqlUtils.deleteSite(wpComSite);

        mSiteSqlUtils.insertOrUpdateSite(wpComSite);
        int affectedRows = mSiteSqlUtils.deleteSite(wpComSite);

        assertEquals(1, affectedRows);
        assertEquals(0, mSiteStore.getSites().size());
    }

    @Test
    public void testGetWPComSites() throws DuplicateSiteException {
        SiteModel wpComSite = generateWPComSite();
        SiteModel jetpackSiteOverXMLRPC = generateJetpackSiteOverXMLRPC();
        SiteModel jetpackSiteOverRestOnly = generateJetpackSiteOverRestOnly();

        mSiteSqlUtils.insertOrUpdateSite(wpComSite);
        mSiteSqlUtils.insertOrUpdateSite(jetpackSiteOverXMLRPC);
        mSiteSqlUtils.insertOrUpdateSite(jetpackSiteOverRestOnly);

        assertEquals(2, mSiteSqlUtils.getSitesAccessedViaWPComRest().getAsCursor().getCount());

        List<SiteModel> wpComSites = mSiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel();
        assertEquals(1, wpComSites.size());
        for (SiteModel site : wpComSites) {
            assertNotEquals(jetpackSiteOverXMLRPC.getId(), site.getId());
        }
    }

    @Test
    public void testInsertDuplicateSites() throws DuplicateSiteException {
        SiteModel futureJetpack = generateSelfHostedSiteFutureJetpack();
        SiteModel jetpack = generateJetpackSiteOverRestOnly();

        // Insert a self hosted site that will later be converted to Jetpack
        mSiteSqlUtils.insertOrUpdateSite(futureJetpack);

        // Insert the same site but Jetpack powered this time
        mSiteSqlUtils.insertOrUpdateSite(jetpack);

        // Previous site should be converted to a Jetpack site and we should see only one site
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        assertEquals(1, sitesCount);

        List<SiteModel> wpComSites = mSiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel();
        assertEquals(0, wpComSites.size());
        assertEquals(1, mSiteSqlUtils.getSitesAccessedViaWPComRest().getAsCursor().getCount());
        List<SiteModel> jetpackSites =
                mSiteSqlUtils.getSitesWith(SiteModelTable.IS_JETPACK_CONNECTED, true).getAsModel();
        assertEquals(jetpack.getSiteId(), jetpackSites.get(0).getSiteId());
        assertTrue(jetpackSites.get(0).isJetpackConnected());
        assertFalse(jetpackSites.get(0).isWPCom());
    }

    @Test
    public void testInsertDuplicateSitesError() throws DuplicateSiteException {
        SiteModel futureJetpack = generateSelfHostedSiteFutureJetpack();
        SiteModel jetpack = generateJetpackSiteOverRestOnly();

        // Insert a Jetpack powered site
        mSiteSqlUtils.insertOrUpdateSite(jetpack);
        boolean duplicate = false;
        try {
            // Insert the same site but via self hosted this time (this should fail)
            mSiteSqlUtils.insertOrUpdateSite(futureJetpack);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        assertTrue(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        assertEquals(1, sitesCount);
    }

    @Test
    public void testInsertDuplicateSitesDifferentSchemesError1() throws DuplicateSiteException {
        SiteModel futureJetpack = generateSelfHostedSiteFutureJetpack();
        SiteModel jetpack = generateJetpackSiteOverRestOnly();

        futureJetpack.setXmlRpcUrl("https://pony.com/xmlrpc.php");
        jetpack.setXmlRpcUrl("http://pony.com/xmlrpc.php");

        // Insert a Jetpack powered site
        mSiteSqlUtils.insertOrUpdateSite(jetpack);
        boolean duplicate = false;
        try {
            // Insert the same site but via self hosted this time (this should fail)
            mSiteSqlUtils.insertOrUpdateSite(futureJetpack);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        assertTrue(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        assertEquals(1, sitesCount);
    }

    @Test
    public void testInsertDuplicateSitesDifferentSchemesError2() throws DuplicateSiteException {
        SiteModel futureJetpack = generateSelfHostedSiteFutureJetpack();
        SiteModel jetpack = generateJetpackSiteOverRestOnly();

        futureJetpack.setXmlRpcUrl("http://pony.com/xmlrpc.php");
        jetpack.setXmlRpcUrl("https://pony.com/xmlrpc.php");

        // Insert a Jetpack powered site
        mSiteSqlUtils.insertOrUpdateSite(jetpack);
        boolean duplicate = false;
        try {
            // Insert the same site but via self hosted this time (this should fail)
            mSiteSqlUtils.insertOrUpdateSite(futureJetpack);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        assertTrue(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        assertEquals(1, sitesCount);
    }

    @Test
    public void testInsertDuplicateXmlRpcJetpackSite() throws DuplicateSiteException {
        SiteModel jetpackXmlRpcSite = generateJetpackSiteOverXMLRPC();

        jetpackXmlRpcSite.setUrl("http://some.url");

        // Insert a Jetpack powered site over XML-RPC
        mSiteSqlUtils.insertOrUpdateSite(jetpackXmlRpcSite);

        // Set up the same site (by URL/XML-RPC URL), but don't identify it as a Jetpack site
        // This simulates sites resulting from wp.getUsersBlogs, which don't have the site ID and can't be identified
        // as Jetpack or not (wp.getOptions is the call that returns that information)
        SiteModel jetpackXmlRpcSite2 = generateSelfHostedNonJPSite();
        jetpackXmlRpcSite2.setXmlRpcUrl(jetpackXmlRpcSite.getXmlRpcUrl());
        jetpackXmlRpcSite2.setUrl(jetpackXmlRpcSite.getUrl());
        jetpackXmlRpcSite2.setSelfHostedSiteId(jetpackXmlRpcSite.getSelfHostedSiteId());
        jetpackXmlRpcSite2.setUsername(jetpackXmlRpcSite.getUsername());
        jetpackXmlRpcSite2.setPassword(jetpackXmlRpcSite.getPassword());

        boolean duplicate = false;
        try {
            // Insert the same site but not identified as a Jetpack site
            // (this should succeed, replacing the existing site, because the site replaced is not using the REST API)
            mSiteSqlUtils.insertOrUpdateSite(jetpackXmlRpcSite2);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        assertFalse(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        assertEquals(1, sitesCount);
    }

    @Test
    public void testSearchSitesByNameMatching() throws DuplicateSiteException {
        SiteModel wpComSite1 = generateWPComSite();
        wpComSite1.setName("Doctor Emmet Brown Homepage");
        SiteModel wpComSite2 = generateWPComSite();
        wpComSite2.setName("Shield Eyes from light");
        wpComSite2.setSiteId(557);
        SiteModel wpComSite3 = generateWPComSite();
        wpComSite3.setName("I remember when this was all farmland as far as the eye could see");
        wpComSite2.setSiteId(558);

        mSiteSqlUtils.insertOrUpdateSite(wpComSite1);
        mSiteSqlUtils.insertOrUpdateSite(wpComSite2);
        mSiteSqlUtils.insertOrUpdateSite(wpComSite3);

        List<SiteModel> matchingSites = mSiteSqlUtils.getSitesByNameOrUrlMatching("eye");
        assertEquals(2, matchingSites.size());

        matchingSites = mSiteSqlUtils.getSitesByNameOrUrlMatching("EYE");
        assertEquals(2, matchingSites.size());
    }

    @Test
    public void testSearchSitesByNameOrUrlMatching() throws DuplicateSiteException {
        SiteModel wpComSite1 = generateWPComSite();
        wpComSite1.setName("Doctor Emmet Brown Homepage");
        SiteModel wpComSite2 = generateWPComSite();
        wpComSite2.setUrl("shieldeyesfromlight.wordpress.com");
        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        selfHostedSite.setName("I remember when this was all farmland as far as the eye could see.");

        mSiteSqlUtils.insertOrUpdateSite(wpComSite1);
        mSiteSqlUtils.insertOrUpdateSite(wpComSite2);
        mSiteSqlUtils.insertOrUpdateSite(selfHostedSite);

        List<SiteModel> matchingSites = mSiteSqlUtils.getSitesByNameOrUrlMatching("eye");
        assertEquals(2, matchingSites.size());

        matchingSites = mSiteSqlUtils.getSitesByNameOrUrlMatching("EYE");
        assertEquals(2, matchingSites.size());
    }

    @Test
    public void testSearchWPComSitesByNameOrUrlMatching() throws DuplicateSiteException {
        SiteModel wpComSite1 = generateWPComSite();
        wpComSite1.setName("Doctor Emmet Brown Homepage");
        SiteModel wpComSite2 = generateWPComSite();
        wpComSite2.setUrl("shieldeyesfromlight.wordpress.com");
        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        selfHostedSite.setName("I remember when this was all farmland as far as the eye could see.");

        mSiteSqlUtils.insertOrUpdateSite(wpComSite1);
        mSiteSqlUtils.insertOrUpdateSite(wpComSite2);
        mSiteSqlUtils.insertOrUpdateSite(selfHostedSite);

        List<SiteModel> matchingSites = mSiteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching("eye");
        assertEquals(1, matchingSites.size());

        matchingSites = mSiteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching("EYE");
        assertEquals(1, matchingSites.size());
    }

    @Test
    public void testRemoveAllSites() throws DuplicateSiteException {
        SiteModel wpComSite = generateWPComSite();
        SiteModel jetpackXMLRPCSite = generateJetpackSiteOverXMLRPC();
        SiteModel jetpackRestSite = generateJetpackSiteOverRestOnly();
        SiteModel selfHostedSite = generateSelfHostedNonJPSite();

        mSiteSqlUtils.insertOrUpdateSite(wpComSite);
        mSiteSqlUtils.insertOrUpdateSite(jetpackXMLRPCSite);
        mSiteSqlUtils.insertOrUpdateSite(jetpackRestSite);
        mSiteSqlUtils.insertOrUpdateSite(selfHostedSite);

        // first make sure sites are inserted successfully
        assertEquals(4, mSiteStore.getSites().size());

        mSiteSqlUtils.deleteAllSites();

        assertEquals(0, mSiteStore.getSites().size());
    }

    @Test
    public void testWPComAutomatedTransfer() throws DuplicateSiteException {
        SiteModel wpComSite = generateWPComSite();
        mSiteSqlUtils.insertOrUpdateSite(wpComSite);

        // Turn WP.com site into an Automated Transfer (Jetpack) site
        SiteModel automatedTransferSite = generateWPComSite();
        automatedTransferSite.setIsJetpackInstalled(true);
        automatedTransferSite.setIsJetpackConnected(true);
        automatedTransferSite.setIsWPCom(false);
        automatedTransferSite.setIsAutomatedTransfer(true);

        mSiteSqlUtils.insertOrUpdateSite(automatedTransferSite);

        assertEquals(1, mSiteStore.getSites().size());
    }

    @Test
    public void testBatchInsertSiteNoDuplicateWPCom()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        List<SiteModel> siteList = new ArrayList<>();
        siteList.add(generateTestSite(1, "https://pony1.com", "https://pony1.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(2, "https://pony2.com", "https://pony2.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(3, "https://pony3.com", "https://pony3.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(4, "https://pony4.com", "https://pony4.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(5, "https://pony5.com", "https://pony5.com/xmlrpc.php", true, true));

        SitesModel sites = new SitesModel(siteList);

        // Use reflection to call a private Store method: equivalent to mSiteStore.updateSites(sites)
        Method createOrUpdateSites = SiteStore.class.getDeclaredMethod("createOrUpdateSites", SitesModel.class);
        createOrUpdateSites.setAccessible(true);
        UpdateSitesResult res = (UpdateSitesResult) createOrUpdateSites.invoke(mSiteStore, sites);

        assertFalse(res.duplicateSiteFound);
        assertEquals(5, res.rowsAffected);
        assertEquals(5, mSiteStore.getSites().size());
    }

    @Test
    public void testInsertSiteDuplicateXmlRpcTrailingSlash() throws DuplicateSiteException {
        // It's possible for the URL in `wp.getOptions` to be different from the URL in `wp.getUsersBlogs`,
        // sometimes just by a trailing slash
        // This test checks that we can still identify two sites as being identical in this case, and that we quietly
        // update the existing site rather than throw a duplicate site exception
        SiteModel selfhostedSite = generateSelfHostedNonJPSite();
        selfhostedSite.setUrl("http://some.url");

        mSiteSqlUtils.insertOrUpdateSite(selfhostedSite);

        SiteModel selfhostedSite2 = generateSelfHostedNonJPSite();
        selfhostedSite2.setUrl("http://some.url/");

        boolean duplicate = false;
        try {
            // Insert the same site with a trailing slash (this should succeed, replacing the existing site)
            mSiteSqlUtils.insertOrUpdateSite(selfhostedSite2);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        assertFalse(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        assertEquals(1, sitesCount);
    }

    @Test
    public void testInsertSiteDuplicateXmlRpcDifferentUrl() throws DuplicateSiteException {
        // It's possible for the URL in `wp.getOptions` to be different from the URL in `wp.getUsersBlogs`
        // This test checks that we can still identify two sites as being identical in this case, and that we quietly
        // update the existing site rather than throw a duplicate site exception
        SiteModel selfhostedSite = generateSelfHostedNonJPSite();
        selfhostedSite.setUrl("http://some.url");
        selfhostedSite.setXmlRpcUrl("http://some.url/xmlrpc.php");

        mSiteSqlUtils.insertOrUpdateSite(selfhostedSite);

        SiteModel selfhostedSite2 = generateSelfHostedNonJPSite();
        selfhostedSite2.setUrl("http://user5242.stagingsite.url");
        selfhostedSite2.setXmlRpcUrl("http://some.url/xmlrpc.php");

        boolean duplicate = false;
        try {
            // Insert the same site with a different URL, but the same XML-RPC URL
            // (this should succeed, replacing the existing site)
            mSiteSqlUtils.insertOrUpdateSite(selfhostedSite2);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        assertFalse(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        assertEquals(1, sitesCount);
    }

    @Test
    public void testUpdateSiteUniqueConstraintFail() throws DuplicateSiteException {
        // Create 2 test sites
        SiteModel site1 = generateTestSite(0, "https://pony1.com", "https://pony1.com/xmlrpc.php", false, true);
        mSiteSqlUtils.insertOrUpdateSite(site1);
        SiteModel site2 = generateTestSite(0, "https://pony2.com", "https://pony2.com/xmlrpc.php", false, true);
        mSiteSqlUtils.insertOrUpdateSite(site2);

        // Update the second site and reuse the site url and id from the first
        site2.setUrl("https://pony1.com");
        boolean duplicate = false;
        try {
            mSiteSqlUtils.insertOrUpdateSite(site2);
        } catch (DuplicateSiteException e) {
            duplicate = true;
        }
        assertTrue(duplicate);
    }

    @Test
    public void testJetpackSelfHostedAndForceXMLRPC() {
        SiteModel jetpackSite = generateJetpackSiteOverXMLRPC();
        jetpackSite.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        assertTrue(jetpackSite.isUsingWpComRestApi());

        // Force the origin, it should now use XMLRPC instead of REST.
        jetpackSite.setOrigin(SiteModel.ORIGIN_XMLRPC);
        assertFalse(jetpackSite.isUsingWpComRestApi());
    }

    @Test
    public void testDefaultUsageWpComRestApi() {
        SiteModel wpComSite = generateWPComSite();
        assertTrue(wpComSite.isUsingWpComRestApi());

        SiteModel jetpack1 = generateJetpackSiteOverRestOnly();
        assertTrue(jetpack1.isUsingWpComRestApi());

        SiteModel jetpack2 = generateJetpackSiteOverXMLRPC();
        assertFalse(jetpack2.isUsingWpComRestApi());

        SiteModel pureSelfHosted1 = generateSelfHostedNonJPSite();
        assertFalse(pureSelfHosted1.isUsingWpComRestApi());

        SiteModel pureSelfHosted2 = generateSelfHostedSiteFutureJetpack();
        assertFalse(pureSelfHosted2.isUsingWpComRestApi());
    }

    @Test
    public void testRemoveWPComRestSitesAbsentFromList()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final List<SiteModel> allSites = new ArrayList<>();
        final List<SiteModel> sitesToKeep = new ArrayList<>();

        for (int i = 0; i < 15; ++i) {
            switch (i % 3) {
                case 0:
                    // add a .com site
                    SiteModel wpComSite = generateWPComSite();
                    wpComSite.setSiteId(i + 1);
                    wpComSite.setUrl("https://pony" + i + ".com");
                    wpComSite.setXmlRpcUrl("https://pony" + i + ".com/xmlrpc.php");
                    allSites.add(wpComSite);
                    break;
                case 1:
                    // add a self-hosted Jetpack site
                    SiteModel jetpackSite = generateJetpackSiteOverRestOnly();
                    jetpackSite.setSiteId(i + 1);
                    jetpackSite.setUrl("https://pony" + i + ".com");
                    jetpackSite.setXmlRpcUrl("https://pony" + i + ".com/xmlrpc.php");
                    allSites.add(jetpackSite);
                    break;
                case 2:
                    // add a self-hosted non-Jetpack site
                    SiteModel selfHostedSite = generateSelfHostedNonJPSite();
                    selfHostedSite.setSiteId(i + 1);
                    selfHostedSite.setUrl("https://pony" + i + ".com");
                    selfHostedSite.setXmlRpcUrl("https://pony" + i + ".com/xmlrpc.php");
                    allSites.add(selfHostedSite);
                    break;
            }
        }

        // add all sites to DB
        Method createOrUpdateSites = SiteStore.class.getDeclaredMethod("createOrUpdateSites", SitesModel.class);
        createOrUpdateSites.setAccessible(true);
        UpdateSitesResult res = (UpdateSitesResult) createOrUpdateSites.invoke(mSiteStore, new SitesModel(allSites));

        assertFalse(res.duplicateSiteFound);
        assertTrue(res.rowsAffected == 15);
        assertTrue(mSiteStore.getSites().size() == 15);

        // add 2 of each kind of site to keep
        sitesToKeep.addAll(allSites.subList(0, 6));

        // remove six sites (2/3 * (15 - 6))
        mSiteSqlUtils.removeWPComRestSitesAbsentFromList(sitesToKeep);

        assertTrue(mSiteStore.getSites().size() == 9);

        // make sure all sites in sitesToKeep are in the store
        for (SiteModel site : sitesToKeep) {
            assertTrue(mSiteStore.getSiteBySiteId(site.getSiteId()) != null);
        }
    }

    @Test
    public void testInsertAndRetrieveForActiveModules() throws DuplicateSiteException {
        SiteModel site = generateWPComSite();
        String activeModules = SiteModel.ACTIVE_MODULES_KEY_PUBLICIZE
                               + ","
                               + SiteModel.ACTIVE_MODULES_KEY_SHARING_BUTTONS;
        site.setActiveModules(activeModules);

        mSiteSqlUtils.insertOrUpdateSite(site);

        SiteModel siteFromDb = mSiteSqlUtils.getSites().get(0);
        assertTrue(siteFromDb.isActiveModuleEnabled(SiteModel.ACTIVE_MODULES_KEY_PUBLICIZE));
        assertTrue(siteFromDb.isActiveModuleEnabled(SiteModel.ACTIVE_MODULES_KEY_SHARING_BUTTONS));
    }

    @Test
    public void testInsertAndRetrieveForPublicizePermanentlyDisabled() throws DuplicateSiteException {
        SiteModel site = generateWPComSite();
        site.setIsPublicizePermanentlyDisabled(true);

        mSiteSqlUtils.insertOrUpdateSite(site);

        SiteModel siteFromDb = mSiteSqlUtils.getSites().get(0);
        assertTrue(siteFromDb.isPublicizePermanentlyDisabled());
    }

    @Test
    public void testZendeskPlanAndAddonsInsertionAndRetrieval() {
        SiteModel siteModel = generateSiteWithZendeskMetaData();
        WellSql.insert(siteModel).execute();

        SiteModel siteFromDb = mSiteStore.getSites().get(0);
        assertEquals(siteModel.getZendeskPlan(), siteFromDb.getZendeskPlan());
        assertEquals(siteModel.getZendeskAddOns(), siteFromDb.getZendeskAddOns());
    }
}
