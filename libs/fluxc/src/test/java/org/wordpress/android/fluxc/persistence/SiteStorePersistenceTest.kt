package org.wordpress.android.fluxc.persistence

import androidx.test.core.app.ApplicationProvider
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel

@RunWith(RobolectricTestRunner::class)
class SiteStorePersistenceTest {
    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    private val accountStorePersistence: AccountStorePersistence = mock {
        on { getDefaultAccount() } doReturn AccountModel().apply { userId = 1L }
    }

    private val siteSqlUtils = SiteSqlUtils()
    private lateinit var sut: SiteStorePersistence

    @Before
    fun setUp() {
        val config = WellSqlConfig(ApplicationProvider.getApplicationContext())
        WellSql.init(config)
        config.reset()

        sut = SiteStorePersistence(accountStorePersistence)
    }

    // region null and account validation

    @Test
    fun `given wpcom site without account, when insert or update, then returns 0`() {
        val noAccountPersistence: AccountStorePersistence = mock {
            on { getDefaultAccount() } doReturn null
        }
        val sutNoAccount = SiteStorePersistence(noAccountPersistence)
        val site = SiteModel().apply {
            siteId = 100
            url = "https://test.com"
            origin = SiteModel.ORIGIN_WPCOM_REST
        }

        val result = sutNoAccount.insertOrUpdateSite(site)

        assertThat(result).isEqualTo(0)
    }

    // endregion

    @Test
    fun `given application-password site local ID, when URL is normalized, then update the existing row`() {
        val site = SiteModel().apply {
            url = "http://test.com"
            origin = SiteModel.ORIGIN_WPAPI
        }
        sut.insertOrUpdateSite(site)
        val originalLocalId = site.id

        site.url = "https://test.com"
        val result = sut.insertOrUpdateSite(site)

        assertThat(result).isEqualTo(1)
        assertThat(site.id).isEqualTo(originalLocalId)
        assertThat(siteSqlUtils.getSites()).singleElement().extracting(SiteModel::getUrl)
            .isEqualTo("https://test.com")
    }

    @Test
    fun `given HTTP and HTTPS rows collide, when normalizing by local ID, then preserve both and fail`() {
        val httpSite = SiteModel().apply { url = "http://test.com" }
        val httpsSite = SiteModel().apply { url = "https://test.com" }
        sut.insertOrUpdateSite(httpSite)
        sut.insertOrUpdateSite(httpsSite)

        httpSite.url = "https://test.com"

        assertThatThrownBy { sut.insertOrUpdateSite(httpSite) }
            .isInstanceOf(SiteStorePersistence.DuplicateSiteException::class.java)
        assertThat(siteSqlUtils.getSites()).hasSize(2)
        assertThat(siteSqlUtils.getSites().map(SiteModel::getUrl))
            .containsExactlyInAnyOrder("http://test.com", "https://test.com")
    }

    // region insert (case 4)

    @Test
    fun `given new site, when insert or update, then inserts and returns 1`() {
        val site = SiteModel().apply {
            siteId = 42
            url = "https://test.com"
        }

        val result = sut.insertOrUpdateSite(site)

        assertThat(result).isEqualTo(1)
        assertThat(siteSqlUtils.getSites()).hasSize(1)
    }

    // endregion

    // region update by local ID (case 1)

    @Test
    fun `given existing site, when insert or update by local id, then updates`() {
        val site = SiteModel().apply {
            siteId = 42
            url = "https://test.com"
            name = "Original"
        }
        sut.insertOrUpdateSite(site)

        site.name = "Updated"
        val result = sut.insertOrUpdateSite(site)

        assertThat(result).isEqualTo(1)
        assertThat(siteSqlUtils.getSites()).hasSize(1)
        assertThat(siteSqlUtils.getSites().first().name).isEqualTo("Updated")
    }

    // endregion

    // region update by remote SITE_ID (case 2)

    @Test
    fun `given existing site, when insert or update by remote id, then updates`() {
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
        assertThat(siteSqlUtils.getSites()).hasSize(1)
        assertThat(siteSqlUtils.getSites().first().name).isEqualTo("Updated via remote ID")
    }

    // endregion

    // region update by SITE_ID + URL for self-hosted (case 3)

    @Test
    fun `given self-hosted site, when insert or update by site id and url, then updates`() {
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
        assertThat(siteSqlUtils.getSites()).hasSize(1)
        assertThat(siteSqlUtils.getSites().first().name).isEqualTo("Updated")
    }

    // endregion

    // region same URL, different origins

    @Test
    fun `given wpcom site exists, when wpapi site with same url inserted, then inserts both`() {
        val wpComSite = SiteModel().apply {
            siteId = 42
            url = "https://test.com"
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        sut.insertOrUpdateSite(wpComSite)

        val wpApiSite = SiteModel().apply {
            siteId = 0
            url = "https://test.com"
            origin = SiteModel.ORIGIN_WPAPI
        }
        val result = sut.insertOrUpdateSite(wpApiSite)

        assertThat(result).isEqualTo(1)
        assertThat(siteSqlUtils.getSites()).hasSize(2)
    }

    @Test
    fun `given wpapi site exists, when wpcom site with same url inserted, then inserts both and keeps wpapi row`() {
        val wpApiSite = SiteModel().apply {
            siteId = 0
            url = "https://test.com"
            origin = SiteModel.ORIGIN_WPAPI
            username = "storeuser"
        }
        sut.insertOrUpdateSite(wpApiSite)

        val wpComSite = SiteModel().apply {
            siteId = 42
            url = "https://test.com"
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        val result = sut.insertOrUpdateSite(wpComSite)

        assertThat(result).isEqualTo(1)
        assertThat(siteSqlUtils.getSites()).hasSize(2)
        val wpApiRow = siteSqlUtils.getSites().first { it.origin == SiteModel.ORIGIN_WPAPI }
        assertThat(wpApiRow.username).isEqualTo("storeuser")
    }

    // endregion

    // region identity crisis — both WP.com with same URL

    @Test
    fun `given wpcom site exists, when another wpcom site with same url, then inserts both`() {
        val site1 = SiteModel().apply {
            siteId = 42
            url = "https://shared.com"
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        sut.insertOrUpdateSite(site1)

        val site2 = SiteModel().apply {
            siteId = 43
            url = "https://shared.com"
            origin = SiteModel.ORIGIN_WPCOM_REST
        }
        val result = sut.insertOrUpdateSite(site2)

        assertThat(result).isEqualTo(1)
        assertThat(siteSqlUtils.getSites()).hasSize(2)
    }

    // endregion
}
