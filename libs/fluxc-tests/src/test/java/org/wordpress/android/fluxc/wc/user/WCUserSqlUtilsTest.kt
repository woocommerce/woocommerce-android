package org.wordpress.android.fluxc.wc.user

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
import org.wordpress.android.fluxc.model.user.WCUserModel
import org.wordpress.android.fluxc.persistence.WCUserSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCUserSqlUtilsTest {
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
                listOf(SiteModel::class.java, WCUserModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()

        // Insert the site into the db so it's available later
        TestSiteSqlUtils.siteSqlUtils.insertOrUpdateSite(site)
    }

    @Test
    fun testInsertOrUpdateUserSite() {
        val userApiResponse = WCUserTestUtils.generateSampleUApiResponse()
        val user = WCUserTestUtils.getSampleUser(userApiResponse, site)

        // Test inserting a user
        var rowsAffected = WCUserSqlUtils.insertOrUpdateUser(user)
        assertEquals(1, rowsAffected)

        var savedUser = WCUserSqlUtils.getUserBySiteAndEmail(site.id, user.email)
        assertEquals(savedUser?.localSiteId, user.localSiteId)
        assertEquals(savedUser?.username, user.username)
        assertEquals(savedUser?.email, user.email)
        assertEquals(savedUser?.roles, user.roles)

        // Test updating the same user
        user.apply {
            username = "testing"
        }
        rowsAffected = WCUserSqlUtils.insertOrUpdateUser(user)
        assertEquals(1, rowsAffected)
        savedUser = WCUserSqlUtils.getUserBySiteAndEmail(site.id, user.email)
        assertEquals(savedUser?.localSiteId, user.localSiteId)
        assertEquals(savedUser?.username, user.username)
        assertEquals(savedUser?.email, user.email)
        assertEquals(savedUser?.roles, user.roles)
        assertTrue(savedUser?.isUserEligible() == true)

        // Test updating user's role
        user.apply { roles = "[\"author\",\"administrator\"]" }
        rowsAffected = WCUserSqlUtils.insertOrUpdateUser(user)
        assertEquals(1, rowsAffected)
        savedUser = WCUserSqlUtils.getUserBySiteAndEmail(site.id, user.email)
        assertTrue(savedUser?.isUserEligible() == true)

        user.apply { roles = "[\"author\",\"customer\"]" }
        rowsAffected = WCUserSqlUtils.insertOrUpdateUser(user)
        assertEquals(1, rowsAffected)
        savedUser = WCUserSqlUtils.getUserBySiteAndEmail(site.id, user.email)
        assertTrue(savedUser?.isUserEligible() == false)

        user.apply { roles = "[\"administrator\",\"shop_manager\"]" }
        rowsAffected = WCUserSqlUtils.insertOrUpdateUser(user)
        assertEquals(1, rowsAffected)
        savedUser = WCUserSqlUtils.getUserBySiteAndEmail(site.id, user.email)
        assertTrue(savedUser?.isUserEligible() == true)
    }

    @Test
    fun testGetUsersForSite() {
        val userApiResponse = WCUserTestUtils.generateSampleUApiResponse()
        val user = WCUserTestUtils.getSampleUser(userApiResponse, site)

        // Insert user
        val rowsAffected = WCUserSqlUtils.insertOrUpdateUser(user)
        assertEquals(1, rowsAffected)

        // Get user for site and verify
        val savedUsersExists = WCUserSqlUtils.getUsersBySite(site.id)
        assertEquals(1, savedUsersExists.size)

        // Get user for a site that does not exist
        val nonExistingSite = SiteModel().apply { id = 400 }
        val savedUsers = WCUserSqlUtils.getUsersBySite(nonExistingSite.id)
        assertEquals(0, savedUsers.size)
    }

    @Test
    fun testDeleteSiteDeletesUserList() {
        val userApiResponse = WCUserTestUtils.generateSampleUApiResponse()
        val user = WCUserTestUtils.getSampleUser(userApiResponse, site)

        // Insert user
        val rowsAffected = WCUserSqlUtils.insertOrUpdateUser(user)
        assertEquals(1, rowsAffected)

        // Get user for site and verify
        var savedUsers = WCUserSqlUtils.getUsersBySite(site.id)
        assertEquals(1, savedUsers.size)

        // Delete site and verify users are deleted via foreign key constraint
        TestSiteSqlUtils.siteSqlUtils.deleteSite(site)
        savedUsers = WCUserSqlUtils.getUsersBySite(site.id)
        assertEquals(0, savedUsers.size)
    }
}
