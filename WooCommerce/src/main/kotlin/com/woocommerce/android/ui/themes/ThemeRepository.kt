package com.woocommerce.android.ui.themes

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.model.Theme
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ThemeActionBuilder
import org.wordpress.android.fluxc.model.ThemeModel
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.fluxc.store.ThemeStore.FetchWPComThemesPayload
import org.wordpress.android.fluxc.store.ThemeStore.OnThemeInstalled
import org.wordpress.android.fluxc.store.ThemeStore.OnWpComThemesChanged
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload
import org.wordpress.android.fluxc.store.ThemeStore.ThemeErrorType
import javax.inject.Inject

class ThemeRepository @Inject constructor(
    private val themeStore: ThemeStore,
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val STORE_THEMES_FILTER = "subject:store"
        private const val STORE_THEMES_LIMIT = 100
        private val supportedThemes by lazy {
            listOf(
                "tsubaki", "tazza", "amulet", "zaino", "thriving-artist", "attar"
            )
        }
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

    suspend fun activateTheme(theme: Theme): Result<Unit> {
        installThemeIfNeeded(theme).onFailure { return Result.failure(it) }

        val activationResult: OnThemeInstalled = dispatcher.dispatchAndAwait(
            ThemeActionBuilder.newActivateThemeAction(
                SiteThemePayload(selectedSite.get(), theme.toThemeModel())
            )
        )

        return when {
            !activationResult.isError -> {
                WooLog.d(WooLog.T.THEMES, "Theme activated successfully: ${theme.id}")
                Result.success(Unit)
            }

            else -> {
                with(activationResult.error) {
                    WooLog.w(WooLog.T.THEMES, "Error activating theme: ${type}, $message")
                }
                Result.failure(OnChangedException(activationResult.error))
            }
        }
    }

    private suspend fun installThemeIfNeeded(theme: Theme): Result<Unit> {
        val installationResult: OnThemeInstalled = dispatcher.dispatchAndAwait(
            ThemeActionBuilder.newInstallThemeAction(
                SiteThemePayload(selectedSite.get(), theme.toThemeModel())
            )
        )

        return when {
            !installationResult.isError || installationResult.error.type == ThemeErrorType.THEME_ALREADY_INSTALLED -> {
                if (installationResult.isError) {
                    WooLog.w(WooLog.T.THEMES, "Theme already installed: ${theme.id}")
                } else {
                    WooLog.d(WooLog.T.THEMES, "Theme installed successfully: ${theme.id}")
                }

                Result.success(Unit)
            }

            else -> {
                with(installationResult.error) {
                    WooLog.w(WooLog.T.THEMES, "Error installing theme: ${type}, $message")
                }
                Result.failure(OnChangedException(installationResult.error))
            }
        }
    }

    private fun Theme.toThemeModel(): ThemeModel {
        return ThemeModel().apply {
            themeId = this@toThemeModel.id
            name = this@toThemeModel.name
            demoUrl = this@toThemeModel.demoUrl
        }
    }
}
