package com.woocommerce.android.ui.login.storesuggestion

import com.woocommerce.android.AppPrefsWrapper
import javax.inject.Inject

class SuggestedStoreLogin @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    fun start(url: String) {
        appPrefsWrapper.suggestedStoreLoginUrl = url
    }

    fun end() {
        appPrefsWrapper.suggestedStoreLoginUrl = ""
    }

    fun isActiveFor(url: String): Boolean {
        val activeUrl = appPrefsWrapper.suggestedStoreLoginUrl
        return activeUrl.isNotEmpty() && SuggestedHostMatcher.isSameHost(activeUrl, url)
    }

    fun onAddressSubmitted(url: String): Boolean {
        val isActive = isActiveFor(url)
        if (!isActive) end()
        return isActive
    }
}
