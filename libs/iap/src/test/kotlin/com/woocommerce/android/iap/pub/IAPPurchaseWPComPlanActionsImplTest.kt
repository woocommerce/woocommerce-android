package com.woocommerce.android.iap.pub

import com.woocommerce.android.iap.internal.core.IAPInMapper
import com.woocommerce.android.iap.internal.core.IAPLifecycleObserver
import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.IAPOutMapper
import com.woocommerce.android.iap.internal.core.IAPPurchasesUpdatedListener
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class IAPPurchaseWPComPlanActionsTest {
    private val logWrapperMock: IAPLogWrapper = mock()
    private val iapMobilePayAPIMock: IAPMobilePayAPI = mock()

    private val sut = IAPPurchaseWPComPlanActionsImpl(
        iapMobilePayAPI = iapMobilePayAPIMock,
        iapManager = buildIapManager()
    )

    private fun buildIapManager(): IAPManager {
        val iapOutMapper = IAPOutMapper()
        val iapPurchasesUpdatedListener = IAPPurchasesUpdatedListener(logWrapperMock)
        val iapLifecycleObserver = IAPLifecycleObserver(
            iapPurchasesUpdatedListener,
            logWrapperMock
        )
        val iapInMapper = IAPInMapper()
        return IAPManager(
            iapLifecycleObserver,
            iapOutMapper,
            iapInMapper,
            iapPurchasesUpdatedListener,
            logWrapperMock,
        )
    }

    @Test
    fun `test run`() = runTest {
        sut.initIAPWithNewActivity(mock())
        sut.isIAPSupported()
    }
}
