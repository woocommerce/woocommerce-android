package com.woocommerce.android.ui.themes

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.model.Theme
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ThemeActionBuilder
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.fluxc.store.ThemeStore.FetchWPComThemesPayload
import org.wordpress.android.fluxc.store.ThemeStore.OnWpComThemesChanged
import javax.inject.Inject

class ThemeRepository @Inject constructor(
    private val themeStore: ThemeStore,
    private val dispatcher: Dispatcher
) {
    companion object {
        private const val STORE_THEMES_FILTER = "subject:store"
        private const val STORE_THEMES_LIMIT = 100
        private val supportedThemes = listOf(
            "tsubaki", "tazza", "amulet", "zaino", "thriving-artist", "attar"
        )
    }

    suspend fun fetchThemes(): Result<List<Theme>> {
        val themesResult: OnWpComThemesChanged = dispatcher.dispatchAndAwait(
            ThemeActionBuilder.newFetchWpComThemesAction(
                FetchWPComThemesPayload(STORE_THEMES_FILTER, STORE_THEMES_LIMIT)
            )
        )

        return if (themesResult.error != null) {
            with(themesResult.error) {
                WooLog.w(WooLog.T.THEMES, "Error fetching themes: ${type}, $message")
            }
            Result.failure(OnChangedException(themesResult.error))
        } else {
            WooLog.d(WooLog.T.THEMES, "Fetched themes successfully")
            themeStore.getWpComThemes(supportedThemes).map { it.toAppModel() }.let {
                if (it.isEmpty()) {
                    WooLog.w(WooLog.T.THEMES, "No themes found")
                    Result.failure(Exception("No themes found"))
                } else {
                    Result.success(it)
                }
            }
        }
    }

    suspend fun getTheme(themeId: String): Theme? = withContext(Dispatchers.IO) {
        themeStore.getWpComThemeByThemeId(themeId)?.toAppModel()
    }
}
