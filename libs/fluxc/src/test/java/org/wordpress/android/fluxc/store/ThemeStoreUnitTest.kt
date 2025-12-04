package org.wordpress.android.fluxc.store

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase
import org.wordpress.android.fluxc.utils.createTestTheme
import org.wordpress.android.fluxc.utils.generateWPComSite
import java.io.IOException
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
            mock(),
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
        val site = generateWPComSite()
        val firstTheme = createTestTheme(site.id, "first-active", "First Active")
        val secondTheme = createTestTheme(site.id, "second-active", "Second Active")

        // Set first theme active and verify
        themeStore.setActiveThemeForSite(site, firstTheme)
        val firstActiveTheme = themeStore.getInstalledThemeByThemeId(site, firstTheme.themeId)
        assertThat(firstActiveTheme!!.active).isTrue

        // Set second theme active and verify
        themeStore.setActiveThemeForSite(site, secondTheme)
        val secondActiveTheme = themeStore.getInstalledThemeByThemeId(site, secondTheme.themeId)
        assertThat(secondActiveTheme!!.active).isTrue

        // Verify first theme is no longer active
        val deactivatedFirstTheme = themeStore.getInstalledThemeByThemeId(site, firstTheme.themeId)
        assertThat(deactivatedFirstTheme!!.active).isFalse
    }

    @Test
    fun `when setting active theme for site, then theme can be retrieved by theme id`() {
        val testThemeId = "fluxc-ftw"
        val testThemeName = "FluxC FTW"
        val testTheme = createTestTheme(0, testThemeId, testThemeName)

        // Verify theme doesn't exist initially
        assertNull(themeStore.getWpComThemeByThemeId(testThemeId))

        // Insert theme via setActiveThemeForSite
        val site = generateWPComSite()
        themeStore.setActiveThemeForSite(site, testTheme)

        // Verify we can retrieve it
        val retrievedTheme = themeStore.getInstalledThemeByThemeId(site, testThemeId)
        assertThat(retrievedTheme)
            .usingRecursiveComparison()
            .ignoringFields("active")
            .isEqualTo(testTheme)
    }
}
