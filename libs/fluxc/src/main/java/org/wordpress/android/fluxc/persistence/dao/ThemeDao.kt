package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.ThemeModel

@Dao
internal abstract class ThemeDao {
    @Query(
        """
        SELECT * FROM ThemeEntity
        WHERE siteId = :siteId
        AND active = 1
        """
    )
    abstract suspend fun getActiveThemesForSite(siteId: LocalId): List<ThemeModel>

    @Query(
        """
        SELECT * FROM ThemeEntity
        WHERE isWpComTheme = 1
        AND themeId IN (:themeIds)
        """
    )
    abstract suspend fun getWpComThemes(themeIds: List<String>): List<ThemeModel>

    @Query(
        """
        SELECT * FROM ThemeEntity
        WHERE themeId = :themeId
        AND isWpComTheme = 1
        LIMIT 1
        """
    )
    abstract suspend fun getWpComThemeByThemeId(themeId: String): ThemeModel?

    @Query(
        """
        SELECT * FROM ThemeEntity
        WHERE siteId = :siteId
        AND themeId = :themeId
        AND isWpComTheme = 0
        LIMIT 1
        """
    )
    abstract suspend fun getSiteThemeByThemeId(
        siteId: LocalId,
        themeId: String
    ): ThemeModel?

    @Transaction
    open suspend fun replaceAllWpComThemes(themes: List<ThemeModel>) {
        deleteWpComThemes()
        upsertThemes(themes)
    }

    @Query("DELETE FROM ThemeEntity WHERE isWpComTheme = 1")
    protected abstract suspend fun deleteWpComThemes()

    @Upsert
    abstract suspend fun upsert(theme: ThemeModel)

    @Upsert
    abstract suspend fun upsertThemes(themes: List<ThemeModel>)
}
