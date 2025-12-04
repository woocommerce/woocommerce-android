package org.wordpress.android.fluxc.theme

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeRestClient
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase
import org.wordpress.android.fluxc.site.SiteUtils
import org.wordpress.android.fluxc.store.ThemeStore
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class ThemeStoreUnitTest {
    private lateinit var database: WPAndroidDatabase
    private lateinit var themeStore: ThemeStore

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        database = Room.inMemoryDatabaseBuilder(
            context, WPAndroidDatabase::class.java
        ).allowMainThreadQueries().build()

        themeStore = ThemeStore(
            Dispatcher(),
            Mockito.mock(ThemeRestClient::class.java),
            database
        )
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun `when setting active theme, then previous theme is deactivated and new theme is activated`() {
        val site = SiteUtils.generateWPComSite()
        val firstTheme = generateTestTheme(site.id, "first-active", "First Active")
        val secondTheme = generateTestTheme(site.id, "second-active", "Second Active")

        // Set first theme active and verify
        themeStore.setActiveThemeForSite(site, firstTheme)
        val firstActiveTheme = themeStore.getInstalledThemeByThemeId(site, firstTheme.themeId)
        assertNotNull(firstActiveTheme)
        assertEquals(firstTheme.themeId, firstActiveTheme.themeId)
        assertEquals(firstTheme.name, firstActiveTheme.name)
        assertThat(firstActiveTheme.active).isTrue

        // Set second theme active and verify
        themeStore.setActiveThemeForSite(site, secondTheme)
        val secondActiveTheme = themeStore.getInstalledThemeByThemeId(site, secondTheme.themeId)
        assertNotNull(secondActiveTheme)
        assertEquals(secondTheme.themeId, secondActiveTheme.themeId)
        assertEquals(secondTheme.name, secondActiveTheme.name)
        assertThat(secondActiveTheme.active).isTrue

        // Verify first theme is no longer active
        val deactivatedFirstTheme = themeStore.getInstalledThemeByThemeId(site, firstTheme.themeId)
        assertNotNull(deactivatedFirstTheme)
        assertThat(deactivatedFirstTheme.active).isFalse
    }

    @Test
    fun `when setting active theme for site, then theme can be retrieved by theme id`() {
        val testThemeId = "fluxc-ftw"
        val testThemeName = "FluxC FTW"
        val testThemes = listOf(generateTestTheme(0, testThemeId, testThemeName))

        // Verify theme doesn't exist initially
        assertNull(themeStore.getWpComThemeByThemeId(testThemeId))

        // Insert theme via setActiveThemeForSite
        val site = SiteUtils.generateWPComSite()
        themeStore.setActiveThemeForSite(site, testThemes[0])

        // Verify we can retrieve it
        val retrievedTheme = themeStore.getInstalledThemeByThemeId(site, testThemeId)
        assertNotNull(retrievedTheme)
        assertEquals(testThemeName, retrievedTheme.name)
    }

    private fun generateTestTheme(siteId: Int, themeId: String, themeName: String) = ThemeModel(
        siteId = LocalId(siteId),
        themeId = themeId,
        name = themeName,
        demoUrl = null,
        active = false,
        isWpComTheme = false
    )
}
