package com.woocommerce.android.ui.prefs.privacy.banner.domain

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.login.AccountRepository
import javax.inject.Inject

class ShouldShowPrivacyBanner @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    private val isUsersCountryGdprCompliant: IsUsersCountryGdprCompliant,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(): Boolean {
        return accountRepository.isUserLoggedIn() &&
            !appPrefs.savedPrivacyBannerSettings &&
            isUsersCountryGdprCompliant()
    }
}
