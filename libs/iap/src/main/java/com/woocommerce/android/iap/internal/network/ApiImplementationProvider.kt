package com.woocommerce.android.iap.internal.network

import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.network.IAPMobilePayAPI
import com.woocommerce.android.iap.pub.network.SandboxTestingConfig

internal class ApiImplementationProvider(private val buildConfigWrapper: SandboxTestingConfig) {
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
}
