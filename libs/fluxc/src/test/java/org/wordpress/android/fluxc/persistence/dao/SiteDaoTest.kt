package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.persistence.entity.SiteEntity

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SiteDaoTest {
    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var dao: SiteDao

    @Before
    fun setUp() {
        dao = wpDatabaseRule.db.siteDao()
    }

    // region Insert and retrieve by local ID

    @Test
    fun `given no site exists, when get by local id, then returns null`() = runTest {
        val result = dao.getByLocalId(1)

        assertThat(result).isNull()
    }

    @Test
    fun `given site inserted, when get by local id, then returns site`() = runTest {
        val sideId = 100L
        val url = "https://example.com"
        val entity = createSiteEntity(siteId = sideId, url = url)
        dao.insert(entity)
        val inserted = dao.getByRemoteId(sideId).first()

        val result = dao.getByLocalId(inserted.id)

        assertThat(result).isNotNull()
        assertThat(result!!.siteId).isEqualTo(100L)
        assertThat(result.url).isEqualTo("https://example.com")
    }

    // endregion

    // region Insert and retrieve by remote ID

    @Test
    fun `given site inserted, when get by remote id, then returns site`() = runTest {
        val sideId = 200L
        val url = "https://remote.com"
        val entity = createSiteEntity(siteId = sideId, url = url)
        dao.insert(entity)

        val result = dao.getByRemoteId(sideId)

        assertThat(result).hasSize(1)
        assertThat(result[0].siteId).isEqualTo(sideId)
        assertThat(result[0].url).isEqualTo(url)
    }

    @Test
    fun `given no site exists, when get by remote id, then returns empty list`() = runTest {
        val result = dao.getByRemoteId(999L)

        assertThat(result).isEmpty()
    }

    // endregion

    // region Get WooCommerce sites

    @Test
    fun `given woo and non-woo sites, when get woo sites, then returns only woo sites`() = runTest {
        val wooSite1 = 1L
        val wooSite2 = 3L
        dao.insert(createSiteEntity(siteId = wooSite1, url = "https://woo.com", hasWooCommerce = true))
        dao.insert(createSiteEntity(siteId = 2L, url = "https://nowoo.com", hasWooCommerce = false))
        dao.insert(createSiteEntity(siteId = wooSite2, url = "https://woo2.com", hasWooCommerce = true))

        val result = dao.getWooSites()

        assertThat(result).hasSize(2)
        assertThat(result.map { it.siteId }).containsExactlyInAnyOrder(wooSite1, wooSite2)
    }

    @Test
    fun `given no woo sites, when get woo sites, then returns empty list`() = runTest {
        dao.insert(createSiteEntity(siteId = 1L, url = "https://nowoo.com", hasWooCommerce = false))

        val result = dao.getWooSites()

        assertThat(result).isEmpty()
    }

    // endregion

    // region Delete by local ID

    @Test
    fun `given site exists, when delete by local id, then site is removed`() = runTest {
        val siteId = 300L
        val entity = createSiteEntity(siteId = siteId, url = "https://delete.com")
        dao.insert(entity)
        val inserted = dao.getByRemoteId(siteId).first()
        assertThat(dao.getByLocalId(inserted.id)).isNotNull()

        dao.deleteByLocalId(inserted.id)

        assertThat(dao.getByLocalId(inserted.id)).isNull()
    }

    // endregion

    // region Delete all

    @Test
    fun `given multiple sites, when delete all, then all sites removed`() = runTest {
        dao.insert(createSiteEntity(siteId = 1L, url = "https://one.com"))
        dao.insert(createSiteEntity(siteId = 2L, url = "https://two.com"))
        dao.insert(createSiteEntity(siteId = 3L, url = "https://three.com"))

        dao.deleteAll()

        assertThat(dao.getAllSites()).isEmpty()
    }

    // endregion

    // region Search by name or URL

    @Test
    fun `given sites with matching names, when search by name, then returns matches`() = runTest {
        val blogSite1 = 1L
        val blogSite2 = 3L
        dao.insert(createSiteEntity(siteId = blogSite1, url = "https://a.com", name = "My Blog"))
        dao.insert(createSiteEntity(siteId = 2L, url = "https://b.com", name = "Another Site"))
        dao.insert(createSiteEntity(siteId = blogSite2, url = "https://blog.example.com", name = "Third"))

        val result = dao.getByNameOrUrlMatching("blog")

        assertThat(result).hasSize(2)
        assertThat(result.map { it.siteId }).containsExactlyInAnyOrder(blogSite1, blogSite2)
    }

    @Test
    fun `given no matching sites, when search, then returns empty list`() = runTest {
        dao.insert(createSiteEntity(siteId = 1L, url = "https://a.com", name = "Site"))

        val result = dao.getByNameOrUrlMatching("nonexistent")

        assertThat(result).isEmpty()
    }

    // endregion

    // region deleteByOriginNotInList

    @Test
    fun `given wpcom sites, when delete absent from list, then only absent deleted`() = runTest {
        val keepSite1 = 1L
        val keepSite2 = 3L
        val origin = SiteModel.ORIGIN_WPCOM_REST
        dao.insert(createSiteEntity(siteId = keepSite1, url = "https://a.com", origin = origin))
        dao.insert(createSiteEntity(siteId = 2L, url = "https://b.com", origin = origin))
        dao.insert(createSiteEntity(siteId = keepSite2, url = "https://c.com", origin = origin))

        dao.deleteByOriginNotInList(origin, listOf(keepSite1, keepSite2))

        assertThat(dao.getAllSites()).hasSize(2)
        assertThat(dao.getAllSites().map { it.siteId }).containsExactlyInAnyOrder(keepSite1, keepSite2)
    }

    // endregion

    /* HELPER */

    @Suppress("LongMethod", "LongParameterList")
    private fun createSiteEntity(
        siteId: Long,
        url: String,
        name: String = "",
        hasWooCommerce: Boolean = false,
        isWPCom: Boolean = false,
        isVisible: Boolean = true,
        origin: Int = 0
    ) = SiteEntity(
        id = 0,
        siteId = siteId,
        url = url,
        adminUrl = "",
        loginUrl = "",
        name = name,
        description = "",
        isWPCom = isWPCom,
        isWPComAtomic = false,
        publishedStatus = 0,
        isFeaturedImageSupported = false,
        isWpForTeamsSite = false,
        defaultCommentStatus = "",
        timezone = "",
        frameNonce = "",
        maxUploadSize = 0L,
        memoryLimit = 0L,
        origin = origin,
        organizationId = 0,
        showOnFront = "",
        pageOnFront = 0L,
        pageForPosts = 0L,
        selfHostedSiteId = 0L,
        username = "",
        password = "",
        xmlRpcUrl = "",
        wpApiRestUrl = "",
        softwareVersion = "",
        isSelfHostedAdmin = false,
        email = "",
        displayName = "",
        isJetpackInstalled = false,
        isJetpackConnected = false,
        isJetpackCPConnected = false,
        jetpackVersion = "",
        jetpackUserEmail = "",
        isAutomatedTransfer = false,
        isWpComStore = false,
        hasWooCommerce = hasWooCommerce,
        isVisible = isVisible,
        isPrivate = false,
        isComingSoon = false,
        isVideoPressSupported = false,
        planId = 0L,
        planShortName = "",
        planProductSlug = "",
        iconUrl = "",
        hasFreePlan = false,
        unmappedUrl = "",
        webEditor = "",
        mobileEditor = "",
        hasCapabilityEditPages = false,
        hasCapabilityEditPosts = false,
        hasCapabilityEditOthersPosts = false,
        hasCapabilityEditOthersPages = false,
        hasCapabilityDeletePosts = false,
        hasCapabilityDeleteOthersPosts = false,
        hasCapabilityEditThemeOptions = false,
        hasCapabilityEditUsers = false,
        hasCapabilityListUsers = false,
        hasCapabilityManageCategories = false,
        hasCapabilityManageOptions = false,
        hasCapabilityActivateWordads = false,
        hasCapabilityPromoteUsers = false,
        hasCapabilityPublishPosts = false,
        hasCapabilityUploadFiles = false,
        hasCapabilityDeleteUser = false,
        hasCapabilityRemoveUsers = false,
        hasCapabilityViewStats = false,
        spaceAvailable = 0L,
        spaceAllowed = 0L,
        spaceUsed = 0L,
        spacePercentUsed = 0.0,
        activeModules = "",
        isPublicizePermanentlyDisabled = false,
        activeJetpackConnectionPlugins = "",
        jetpackModules = "",
        zendeskPlan = "",
        zendeskAddOns = "",
        isBloggingPromptsOptedIn = false,
        isBloggingPromptsCardOptedIn = false,
        isPotentialBloggingSite = false,
        isBloggingReminderOnMonday = false,
        isBloggingReminderOnTuesday = false,
        isBloggingReminderOnWednesday = false,
        isBloggingReminderOnThursday = false,
        isBloggingReminderOnFriday = false,
        isBloggingReminderOnSaturday = false,
        isBloggingReminderOnSunday = false,
        bloggingReminderHour = 0,
        bloggingReminderMinute = 0,
        applicationPasswordsAuthorizeUrl = "",
        canBlaze = false,
        planActiveFeatures = "",
        wasEcommerceTrial = null,
        isSingleUserSite = null,
        isGardenSite = false,
        gardenName = "",
        gardenPartner = ""
    )
}
