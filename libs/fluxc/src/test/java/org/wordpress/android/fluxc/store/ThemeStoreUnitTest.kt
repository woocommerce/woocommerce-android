package org.wordpress.android.fluxc.store

import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.site.SiteUtils
import org.wordpress.android.fluxc.utils.createTestTheme

@RunWith(RobolectricTestRunner::class)
class ThemeStoreUnitTest {
    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(
        ApplicationProvider.getApplicationContext()
    )

    private lateinit var themeStore: ThemeStore

    @Before
    fun setUp() {
        themeStore = ThemeStore(
            Dispatcher(),
            mock(),
            wpDatabaseRule.db
        )
    }

    @Test
    fun `when setting active theme, then previous theme is deactivated and new theme is activated`() {
        val site = SiteUtils.generateWPComSite()
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
        val site = SiteUtils.generateWPComSite()
        val testTheme = createTestTheme(0, testThemeId, testThemeName) // id is set in setActiveThemeForSite

        themeStore.setActiveThemeForSite(site, testTheme)

        val retrievedTheme = themeStore.getInstalledThemeByThemeId(site, testThemeId)
        assertThat(retrievedTheme)
            .usingRecursiveComparison()
            .ignoringFields("active")
            .isEqualTo(testTheme)
    }
}
