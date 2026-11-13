package com.woocommerce.android.ui.aiassistant.appfunctions

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.aiassistant.appfunctions.AppFunctionsAccessPolicy
import com.woocommerce.android.aiassistant.appfunctions.StoreAppFunctionsAvailability
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.util.BuildConfigWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooAppFunctionsAccess @Inject constructor(
    private val selectedSite: SelectedSite,
    private val accountRepository: AccountRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val storeAppFunctionsAvailability: StoreAppFunctionsAvailability,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) : AppFunctionsAccessPolicy {
    private val merchantSwitch = MutableStateFlow(appPrefsWrapper.isAppFunctionsEnabled)

    override fun isAllowed(): Boolean =
        buildConfigWrapper.debug &&
            merchantSwitch.value &&
            selectedSite.getIfExists() != null &&
            accountRepository.isUserLoggedIn()

    fun init() {
        if (!buildConfigWrapper.debug) return

        appCoroutineScope.launch {
            combine(selectedSite.observe(), merchantSwitch) { _, _ -> isAllowed() }
                .collectLatest { storeAppFunctionsAvailability.setEnabled(it) }
        }
    }

    fun onMerchantSwitchChanged(enabled: Boolean) {
        appPrefsWrapper.isAppFunctionsEnabled = enabled
        merchantSwitch.value = enabled
    }
}
