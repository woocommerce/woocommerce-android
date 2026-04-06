package org.wordpress.android.fluxc.persistence

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteStorePersistence.DuplicateSiteException
import org.wordpress.android.fluxc.persistence.dao.SiteDao

@RunWith(RobolectricTestRunner::class)
class SiteStorePersistenceTest {
    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    private lateinit var siteDao: SiteDao
    private val mapper = SiteMapper()
    private val accountStorePersistence: AccountStorePersistence = mock {
        on { getDefaultAccount() } doReturn AccountModel().apply { userId = 1L }
    }

    private lateinit var sut: SiteStorePersistence

    @Before
    fun setUp() {
        siteDao = wpDatabaseRule.db.siteDao()
        sut = SiteStorePersistence(
            siteDao = siteDao,
            siteMapper = mapper,
            accountStorePersistence = accountStorePersistence,
        )
    }

    // region null and account validation

    @Test
    fun `given wpcom site without account, when insert or update, then returns 0`() = runTest {
        val noAccountPersistence: AccountStorePersistence = mock {
            on { getDefaultAccount() } doReturn null
        }
        val sutNoAccount = SiteStorePersistence(
            siteDao = siteDao,
            siteMapper = mapper,
            accountStorePersistence = noAccountPersistence,
        )
        val site = SiteModel().apply {
            siteId = 100
            url = "https://test.com"
            origin = SiteModel.ORIGIN_WPCOM_REST
        }

        val result = sutNoAccount.insertOrUpdateSite(site)

        assertThat(result).isEqualTo(0)
    }

    // endregion

    // region insert (case 6)

    @Test
    fun `given new site, when insert or update, then inserts and returns 1`() = runTest {
        val site = SiteModel().apply {
            siteId = 42
            url = "https://test.com"
        }

        val result = sut.insertOrUpdateSite(site)

        assertThat(result).isEqualTo(1)
        assertThat(siteDao.getAllSites()).hasSize(1)
    }

    // endregion

    // region update by local ID (case 1)

    @Test
    fun `given existing site, when insert or update by local id, then updates`() = runTest {
        val site = SiteModel().apply {
            siteId = 42
            url = "https://test.com"
            name = "Original"
        }
        sut.insertOrUpdateSite(site)

        site.name = "Updated"
        val result = sut.insertOrUpdateSite(site)

        assertThat(result).isEqualTo(1)
        assertThat(siteDao.getAllSites()).hasSize(1)
        assertThat(mapper.toModel(siteDao.getByLocalId(site.id)!!).name).isEqualTo("Updated")
    }

    // endregion

    // region update by remote SITE_ID (case 2)

    @Test
    fun `given existing site, when insert or update by remote id, then updates`() = runTest {
        val original = SiteModel().apply {
            siteId = 42
            url = "https://test.com"
            name = "Original"
        }
        sut.insertOrUpdateSite(original)

        val incoming = SiteModel().apply {
            siteId = 42
            url = "https://test.com"
            name = "Updated via remote ID"
        }
        val result = sut.insertOrUpdateSite(incoming)

        assertThat(result).isEqualTo(1)
        assertThat(siteDao.getAllSites()).hasSize(1)
        assertThat(mapper.toModel(siteDao.getAllSites().first()).name).isEqualTo("Updated via remote ID")
    }

    // endregion

    // region update by SITE_ID + URL for self-hosted (case 3)

    @Test
    fun `given self-hosted site, when insert or update by site id and url, then updates`() = runTest {
        val original = SiteModel().apply {
            siteId = 0
            url = "https://selfhosted.com"
            name = "Original"
        }
        sut.insertOrUpdateSite(original)

        val incoming = SiteModel().apply {
            siteId = 0
            url = "https://selfhosted.com"
            name = "Updated"
        }
        val result = sut.insertOrUpdateSite(incoming)

        assertThat(result).isEqualTo(1)
        assertThat(siteDao.getAllSites()).hasSize(1)
        assertThat(mapper.toModel(siteDao.getAllSites().first()).name).isEqualTo("Updated")
    }

    // endregion

    // region duplicate site exception (case 4)

    @Test(expected = DuplicateSiteException::class)
    fun `given wpcom site exists, when xmlrpc site with same url inserted, then throws`() = runTest {
        val wpComSite = SiteModel().apply {
            siteId = 42
            url = "https://test.com"
            xmlRpcUrl = "https://test.com/xmlrpc.php"
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        sut.insertOrUpdateSite(wpComSite)

        val xmlRpcSite = SiteModel().apply {
            siteId = 0
            url = "https://selfhosted.com"
            xmlRpcUrl = "https://test.com/xmlrpc.php"
            origin = SiteModel.ORIGIN_XMLRPC
        }
        sut.insertOrUpdateSite(xmlRpcSite)
    }

    // endregion

    // region update by XMLRPC_URL for XML-RPC site (case 5)

    @Test
    fun `given xmlrpc site exists, when xmlrpc site with same url inserted, then updates`() = runTest {
        val original = SiteModel().apply {
            siteId = 0
            url = "https://selfhosted.com"
            xmlRpcUrl = "https://selfhosted.com/xmlrpc.php"
            origin = SiteModel.ORIGIN_XMLRPC
        }
        sut.insertOrUpdateSite(original)

        val incoming = SiteModel().apply {
            siteId = 0
            url = "https://selfhosted.com"
            xmlRpcUrl = "https://selfhosted.com/xmlrpc.php"
            origin = SiteModel.ORIGIN_XMLRPC
            name = "Updated"
        }
        val result = sut.insertOrUpdateSite(incoming)

        assertThat(result).isEqualTo(1)
        assertThat(siteDao.getAllSites()).hasSize(1)
    }

    // endregion

    // region identity crisis — both WP.com with same XMLRPC URL

    @Test
    fun `given wpcom site exists, when another wpcom site with same xmlrpc url, then inserts both`() = runTest {
        val site1 = SiteModel().apply {
            siteId = 42
            url = "https://site1.com"
            xmlRpcUrl = "https://shared.com/xmlrpc.php"
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        sut.insertOrUpdateSite(site1)

        val site2 = SiteModel().apply {
            siteId = 43
            url = "https://site2.com"
            xmlRpcUrl = "https://shared.com/xmlrpc.php"
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        val result = sut.insertOrUpdateSite(site2)

        assertThat(result).isEqualTo(1)
        assertThat(siteDao.getAllSites()).hasSize(2)
    }

    // endregion

    // region XMLRPC URL http/https matching

    @Test
    fun `given site with http xmlrpc url, when matching with https variant, then updates`() = runTest {
        val original = SiteModel().apply {
            siteId = 0
            url = "http://selfhosted.com"
            xmlRpcUrl = "http://selfhosted.com/xmlrpc.php"
            origin = SiteModel.ORIGIN_XMLRPC
        }
        sut.insertOrUpdateSite(original)

        val incoming = SiteModel().apply {
            siteId = 0
            url = "https://other.com"
            xmlRpcUrl = "https://selfhosted.com/xmlrpc.php"
            origin = SiteModel.ORIGIN_XMLRPC
        }
        val result = sut.insertOrUpdateSite(incoming)

        assertThat(result).isEqualTo(1)
        assertThat(siteDao.getAllSites()).hasSize(1)
    }

    // endregion
}
