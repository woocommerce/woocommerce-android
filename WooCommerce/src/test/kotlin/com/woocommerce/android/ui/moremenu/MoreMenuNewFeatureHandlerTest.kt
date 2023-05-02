package com.woocommerce.android.ui.moremenu

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MoreMenuNewFeatureHandlerTest : BaseUnitTest() {
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus = mock()

    @Test
    fun `given new feature is not seen and ttp available, when checking state, then returns not empty list`() =
        testBlocking {
            // GIVEN
            whenever(tapToPayAvailabilityStatus()).thenReturn(TapToPayAvailabilityStatus.Result.Available)
            whenever(appPrefsWrapper.observePrefs()).thenReturn(MutableStateFlow(Unit))
            whenever(appPrefsWrapper.isUserSeenNewFeatureOnMoreScreen()).thenReturn(false)
            val moreMenuNewFeatureHandler = MoreMenuNewFeatureHandler(appPrefsWrapper, tapToPayAvailabilityStatus)

            // WHEN && THEN
            assertThat(moreMenuNewFeatureHandler.moreMenuNewFeaturesAvailable.first()).isNotEmpty
        }

    @Test
    fun `given new feature is not seen and ttp not available, when checking state, then returns empty list`() =
        testBlocking {
            // GIVEN
            whenever(tapToPayAvailabilityStatus()).thenReturn(
                TapToPayAvailabilityStatus.Result.NotAvailable.NfcNotAvailable
            )
            whenever(appPrefsWrapper.observePrefs()).thenReturn(MutableStateFlow(Unit))
            whenever(appPrefsWrapper.isUserSeenNewFeatureOnMoreScreen()).thenReturn(false)
            val moreMenuNewFeatureHandler = MoreMenuNewFeatureHandler(appPrefsWrapper, tapToPayAvailabilityStatus)

            // WHEN && THEN
            assertThat(moreMenuNewFeatureHandler.moreMenuNewFeaturesAvailable.first()).isEmpty()
        }

    @Test
    fun `given new feature is seen, when checking state, then returns empty list`() = testBlocking {
        // GIVEN
        whenever(appPrefsWrapper.observePrefs()).thenReturn(MutableStateFlow(Unit))
        whenever(appPrefsWrapper.isUserSeenNewFeatureOnMoreScreen()).thenReturn(true)
        val moreMenuNewFeatureHandler = MoreMenuNewFeatureHandler(appPrefsWrapper, tapToPayAvailabilityStatus)

        // WHEN && THEN
        assertThat(moreMenuNewFeatureHandler.moreMenuNewFeaturesAvailable.first()).isEmpty()
    }

    @Test
    fun `given more menu payments was clicked, when moreMenuPaymentsFeatureWasClicked, then returns true`() =
        testBlocking {
            // GIVEN
            whenever(appPrefsWrapper.observePrefs()).thenReturn(MutableStateFlow(Unit))
            whenever(appPrefsWrapper.isPaymentsIconWasClickedOnMoreScreen()).thenReturn(true)
            val moreMenuNewFeatureHandler = MoreMenuNewFeatureHandler(appPrefsWrapper, tapToPayAvailabilityStatus)

            // WHEN && THEN
            assertThat(moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked.first()).isTrue
        }

    @Test
    fun `given more menu payments was not clicked, when moreMenuPaymentsFeatureWasClicked, then returns false`() =
        testBlocking {
            // GIVEN
            whenever(appPrefsWrapper.observePrefs()).thenReturn(MutableStateFlow(Unit))
            whenever(appPrefsWrapper.isPaymentsIconWasClickedOnMoreScreen()).thenReturn(false)
            val moreMenuNewFeatureHandler = MoreMenuNewFeatureHandler(appPrefsWrapper, tapToPayAvailabilityStatus)

            // WHEN && THEN
            assertThat(moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked.first()).isFalse
        }
}
