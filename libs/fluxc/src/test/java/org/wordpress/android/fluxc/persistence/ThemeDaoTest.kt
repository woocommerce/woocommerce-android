package org.wordpress.android.fluxc.persistence

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.persistence.dao.ThemeDao
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class ThemeDaoTest {
    private lateinit var dao: ThemeDao
    private lateinit var db: WPAndroidDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(
            context, WPAndroidDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.themeDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `when upserting new theme, then theme is inserted`(): Unit = runBlocking {
        // given
        val theme = createTheme(SITE_ID_1, THEME_ID_1, "Theme 1")

        // when
        dao.upsert(theme)

        // then
        val result = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), THEME_ID_1)
        assertThat(result).isNotNull
        assertThat(result?.themeId).isEqualTo(THEME_ID_1)
        assertThat(result?.name).isEqualTo("Theme 1")
        assertThat(result?.siteId).isEqualTo(LocalId(SITE_ID_1))
        assertThat(result?.isWpComTheme).isFalse
    }

    @Test
    fun `when upserting existing theme, then theme is updated`(): Unit = runBlocking {
        // given
        val theme = createTheme(SITE_ID_1, THEME_ID_1, "Theme 1")
        dao.upsert(theme)

        // when
        val updatedTheme = theme.copy(name = "Updated Theme")
        dao.upsert(updatedTheme)

        // then
        val result = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), THEME_ID_1)
        assertThat(result).isNotNull
        assertThat(result?.name).isEqualTo("Updated Theme")
    }

    @Test
    fun `when replacing all WP-com themes, then all previous WP-com themes are replaced`(): Unit = runBlocking {
        // given
        val theme1 = createTheme(0, THEME_ID_1, "WP.com Theme 1", isWpComTheme = true)
        val theme2 = createTheme(0, THEME_ID_2, "WP.com Theme 2", isWpComTheme = true)
        dao.replaceAllWpComThemes(listOf(theme1, theme2))

        // when
        val newTheme = createTheme(0, THEME_ID_3, "New WP.com Theme", isWpComTheme = true)
        dao.replaceAllWpComThemes(listOf(newTheme))

        // then
        val result = dao.getWpComThemes(listOf(THEME_ID_1, THEME_ID_2, THEME_ID_3))
        assertThat(result).hasSize(1)
        assertThat(result[0].themeId).isEqualTo(THEME_ID_3)
        assertThat(result[0].isWpComTheme).isTrue
    }

    @Test
    fun `when upserting themes with different active states, then previous active theme is deactivated and new one is activated`(): Unit = runBlocking {
        // given
        val theme1 = createTheme(SITE_ID_1, THEME_ID_1, "Theme 1", active = true)
        val theme2 = createTheme(SITE_ID_1, THEME_ID_2, "Theme 2")
        dao.upsert(theme1)

        // when - deactivate first and activate second
        dao.upsertThemes(listOf(
            theme1.copy(active = false),
            theme2.copy(active = true)
        ))

        // then
        val oldActiveTheme = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), THEME_ID_1)
        val newActiveTheme = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), THEME_ID_2)
        assertThat(oldActiveTheme?.active).isFalse
        assertThat(newActiveTheme?.active).isTrue
    }

    @Test
    fun `when getting active themes for site, then only active themes are returned`(): Unit = runBlocking {
        // given
        val activeTheme = createTheme(SITE_ID_1, THEME_ID_1, "Active Theme", active = true)
        val inactiveTheme = createTheme(SITE_ID_1, THEME_ID_2, "Inactive Theme", active = false)
        dao.upsertThemes(listOf(activeTheme, inactiveTheme))

        // when
        val result = dao.getActiveThemesForSite(LocalId(SITE_ID_1))

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].themeId).isEqualTo(THEME_ID_1)
        assertThat(result[0].active).isTrue
    }

    @Test
    fun `when getting WP-com themes by IDs, then themes with matching IDs are returned`(): Unit = runBlocking {
        // given
        val theme1 = createTheme(0, THEME_ID_1, "WP.com Theme 1", isWpComTheme = true)
        val theme2 = createTheme(0, THEME_ID_2, "WP.com Theme 2", isWpComTheme = true)
        val theme3 = createTheme(0, THEME_ID_3, "WP.com Theme 3", isWpComTheme = true)
        dao.replaceAllWpComThemes(listOf(theme1, theme2, theme3))

        // when
        val result = dao.getWpComThemes(listOf(THEME_ID_1, THEME_ID_3))

        // then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.themeId }).containsExactlyInAnyOrder(THEME_ID_1, THEME_ID_3)
    }

    @Test
    fun `when getting WP-com theme by theme ID, then correct theme is returned`(): Unit = runBlocking {
        // given
        val theme = createTheme(0, THEME_ID_1, "WP.com Theme", isWpComTheme = true)
        dao.replaceAllWpComThemes(listOf(theme))

        // when
        val result = dao.getWpComThemeByThemeId(THEME_ID_1)

        // then
        assertThat(result).isNotNull
        assertThat(result?.themeId).isEqualTo(THEME_ID_1)
        assertThat(result?.name).isEqualTo("WP.com Theme")
    }

    @Test
    fun `when getting WP-com theme by non-existent theme ID, then null is returned`(): Unit = runBlocking {
        // when
        val result = dao.getWpComThemeByThemeId("non-existent")

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `when getting site theme by theme ID, then correct theme is returned`(): Unit = runBlocking {
        // given
        val theme = createTheme(SITE_ID_1, THEME_ID_1, "Site Theme")
        dao.upsert(theme)

        // when
        val result = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), THEME_ID_1)

        // then
        assertThat(result).isNotNull
        assertThat(result?.themeId).isEqualTo(THEME_ID_1)
        assertThat(result?.name).isEqualTo("Site Theme")
    }

    @Test
    fun `when getting site theme by non-existent theme ID, then null is returned`(): Unit = runBlocking {
        // when
        val result = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), "non-existent")

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `when getting themes for different sites, then themes are isolated by site`(): Unit = runBlocking {
        // given
        val theme = createTheme(SITE_ID_1, THEME_ID_1, "Theme 1")
        dao.upsert(theme)

        // when
        val resultSite1 = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), THEME_ID_1)
        val resultSite2 = dao.getSiteThemeByThemeId(LocalId(SITE_ID_2), THEME_ID_1)

        // then
        assertThat(resultSite1).isNotNull
        assertThat(resultSite2).isNull()
    }

    @Test
    fun `when storing WP-com and site themes with same ID, then themes are separated`(): Unit = runBlocking {
        // given
        val siteTheme = createTheme(SITE_ID_1, THEME_ID_1, "Site Theme")
        val wpComTheme = createTheme(0, THEME_ID_1, "WP.com Theme", isWpComTheme = true)
        dao.upsert(siteTheme)
        dao.replaceAllWpComThemes(listOf(wpComTheme))

        // when
        val siteResult = dao.getSiteThemeByThemeId(LocalId(SITE_ID_1), THEME_ID_1)
        val wpComResult = dao.getWpComThemeByThemeId(THEME_ID_1)

        // then
        assertThat(siteResult).isNotNull
        assertThat(siteResult?.name).isEqualTo("Site Theme")
        assertThat(siteResult?.isWpComTheme).isFalse
        assertThat(wpComResult).isNotNull
        assertThat(wpComResult?.name).isEqualTo("WP.com Theme")
        assertThat(wpComResult?.isWpComTheme).isTrue
    }

    private fun createTheme(
        siteId: Int,
        themeId: String,
        name: String,
        isWpComTheme: Boolean = false,
        active: Boolean = false
    ) = ThemeModel(
        siteId = LocalId(siteId),
        themeId = themeId,
        name = name,
        demoUrl = null,
        active = active,
        isWpComTheme = isWpComTheme
    )

    private companion object {
        const val SITE_ID_1 = 1
        const val SITE_ID_2 = 2
        const val THEME_ID_1 = "theme-1"
        const val THEME_ID_2 = "theme-2"
        const val THEME_ID_3 = "theme-3"
    }
}
