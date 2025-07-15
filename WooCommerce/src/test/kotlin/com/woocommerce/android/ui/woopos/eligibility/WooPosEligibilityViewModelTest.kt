package com.woocommerce.android.ui.woopos.eligibility

import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosEligibilityViewModelTest {

    private val canBeLaunchedInTab: WooPosCanBeLaunchedInTab = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Test
    fun `given POS is eligible on retry, should update state to Eligible`() = runTest {
        // GIVEN
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(WooPosLaunchability.Launchable)
        val sut = createSut()

        // WHEN
        sut.retryEligibilityCheck()

        // THEN
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()
        assertThat(sut.retryState.value).isEqualTo(WooPosEligibilityRetryState.Eligible)
    }

    @Test
    fun `given POS is ineligible on retry, should update state to Ineligible`() = runTest {
        // GIVEN
        whenever(canBeLaunchedInTab(forceRefresh = true)).thenReturn(
            WooPosLaunchability.NotLaunchable(WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency)
        )
        val sut = createSut()

        // WHEN
        sut.retryEligibilityCheck()

        // THEN
        coroutinesTestRule.testDispatcher.scheduler.advanceUntilIdle()
        assertThat(sut.retryState.value).isEqualTo(WooPosEligibilityRetryState.Ineligible)
    }

    private fun createSut() = WooPosEligibilityViewModel(canBeLaunchedInTab)
}
