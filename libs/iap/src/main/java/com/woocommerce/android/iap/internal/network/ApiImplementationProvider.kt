package com.woocommerce.android.iap.internal.network

import com.woocommerce.android.iap.BuildConfig
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.network.IAPMobilePayAPI

internal class ApiImplementationProvider(private val buildConfigWrapper: BuildConfigWrapper = BuildConfigWrapper) {
    fun providerMobilePayAPI(
        logWrapper: IAPLogWrapper,
        realMobilePayApiProvider: (String?) -> IAPMobilePayAPI
    ): IAPMobilePayAPI =
        if (buildConfigWrapper.iapTestingSandboxUrl.isNotEmpty()) {
            realMobilePayApiProvider(buildConfigWrapper.iapTestingSandboxUrl)
        } else {
            if (buildConfigWrapper.isDebug) {
                IAPMobilePayAPIStub(logWrapper)
            } else {
                realMobilePayApiProvider(null)
            }
        }

    object BuildConfigWrapper {
        val isDebug = BuildConfig.DEBUG
        val iapTestingSandboxUrl
            get() = BuildConfig.IAP_TESTING_SANDBOX_URL
    }
}
