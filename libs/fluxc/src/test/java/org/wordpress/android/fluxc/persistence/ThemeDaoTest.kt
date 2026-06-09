package org.wordpress.android.fluxc.persistence

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.dao.ThemeDao
import org.wordpress.android.fluxc.utils.createTestTheme

@RunWith(RobolectricTestRunner::class)
class ThemeDaoTest {
    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var dao: ThemeDao

    @Before
    fun setUp() {
        dao = wpDatabaseRule.db.themeDao()
    }

    @Test
    fun `when upserting new theme, then theme is inserted`(): Unit = runTest {
        val theme = createTestTheme(SITE_ID_1, THEME_ID_1, "Theme 1")

        dao.upsert(theme)

        val result = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), THEME_ID_1)
        assertThat(result).isEqualTo(theme)
    }

    @Test
    fun `when upserting existing theme, then theme is updated`(): Unit = runTest {
        val theme = createTestTheme(SITE_ID_1, THEME_ID_1, "Theme 1")
        dao.upsert(theme)

        val updatedTheme = theme.copy(name = "Updated Theme")
        dao.upsert(updatedTheme)

        val result = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), THEME_ID_1)
        assertThat(result).isEqualTo(updatedTheme)
    }

    @Test
    fun `when replacing all WP-com themes, then all previous WP-com themes are replaced`(): Unit = runTest {
        val theme1 = createTestTheme(0, THEME_ID_1, "WP.com Theme 1", isWpComTheme = true)
        val theme2 = createTestTheme(0, THEME_ID_2, "WP.com Theme 2", isWpComTheme = true)
        dao.replaceAllWpComThemes(listOf(theme1, theme2))

        val newTheme = createTestTheme(0, THEME_ID_3, "New WP.com Theme", isWpComTheme = true)
        dao.replaceAllWpComThemes(listOf(newTheme))

        val result = dao.getWpComThemes(listOf(THEME_ID_1, THEME_ID_2, THEME_ID_3))
        assertThat(result).containsOnly(newTheme)
    }

    @Test
    fun `when getting active themes for site, then only active themes are returned`(): Unit = runTest {
        val activeTheme = createTestTheme(SITE_ID_1, THEME_ID_1, "Active Theme", active = true)
        val inactiveTheme = createTestTheme(SITE_ID_1, THEME_ID_2, "Inactive Theme", active = false)
        dao.upsertThemes(listOf(activeTheme, inactiveTheme))

        val result = dao.getActiveThemesForSite(LocalId(SITE_ID_1))

        assertThat(result).containsOnly(activeTheme)
    }

    @Test
    fun `when getting WP-com themes by IDs, then themes with matching IDs are returned`(): Unit = runTest {
        val theme1 = createTestTheme(0, THEME_ID_1, "WP.com Theme 1", isWpComTheme = true)
        val theme2 = createTestTheme(0, THEME_ID_2, "WP.com Theme 2", isWpComTheme = true)
        val theme3 = createTestTheme(0, THEME_ID_3, "WP.com Theme 3", isWpComTheme = true)
        dao.replaceAllWpComThemes(listOf(theme1, theme2, theme3))

        val result = dao.getWpComThemes(listOf(THEME_ID_1, THEME_ID_3))

        assertThat(result).containsOnly(theme1, theme3)
    }

    @Test
    fun `when getting WP-com theme by theme ID, then correct theme is returned`(): Unit = runTest {
        val theme = createTestTheme(0, THEME_ID_1, "WP.com Theme", isWpComTheme = true)
        dao.replaceAllWpComThemes(listOf(theme))

        val result = dao.getWpComThemeByThemeId(THEME_ID_1)

        assertThat(result).isEqualTo(theme)
    }

    @Test
    fun `when getting WP-com theme by non-existent theme ID, then null is returned`(): Unit = runTest {
        val result = dao.getWpComThemeByThemeId("non-existent")

        assertThat(result).isNull()
    }

    @Test
    fun `when getting site theme by theme ID, then correct theme is returned`(): Unit = runTest {
        val theme = createTestTheme(SITE_ID_1, THEME_ID_1, "Site Theme")
        dao.upsert(theme)

        val result = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), THEME_ID_1)

        assertThat(result).isEqualTo(theme)
    }

    @Test
    fun `when getting site theme by non-existent theme ID, then null is returned`(): Unit = runTest {
        val result = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), "non-existent")

        assertThat(result).isNull()
    }

    private companion object {
        const val SITE_ID_1 = 1
        const val THEME_ID_1 = "theme-1"
        const val THEME_ID_2 = "theme-2"
        const val THEME_ID_3 = "theme-3"
    }
}
