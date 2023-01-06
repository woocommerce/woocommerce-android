package com.woocommerce.android.ui.login.error.notwoo

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class LoginNotWooViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wpApiSiteRepository: WPApiSiteRepository,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val INSTALL_PATH = "plugin-install.php?tab=plugin-information&plugin=woocommerce"
        private const val INSTALLATION_FLAG_KEY = "installation-attempted"
    }

    private val siteUrl
        get() = savedState.get<String>(LoginNotWooDialogFragment.SITE_URL_KEY)!!

    private var hasOpenedInstallationPage: Boolean
        get() = savedState[INSTALLATION_FLAG_KEY] ?: false
        set(value) = savedState.set(INSTALLATION_FLAG_KEY, value)

    fun openWooInstallationScreen() = launch {
        val site = wpApiSiteRepository.getSiteByUrl(siteUrl)

        val installationUrl = (site?.adminUrl ?: siteUrl.slashJoin("wp-admin")).slashJoin(INSTALL_PATH)

        triggerEvent(LaunchWooInstallation(installationUrl))
        hasOpenedInstallationPage = true
    }

    fun onResume() {
        if (hasOpenedInstallationPage) {
            triggerEvent(ExitWithResult(Unit))
        }
    }

    data class LaunchWooInstallation(val installationUrl: String) : MultiLiveEvent.Event()
}
